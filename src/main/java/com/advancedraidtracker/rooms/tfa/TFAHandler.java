package com.advancedraidtracker.rooms.tfa;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.AdvancedRaidTrackerPlugin;
import com.advancedraidtracker.constants.LogID;
import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.constants.RaidType;
import com.advancedraidtracker.constants.TfaIDs;
import com.advancedraidtracker.rooms.RoomHandler;
import com.advancedraidtracker.utility.RoomUtil;
import com.advancedraidtracker.utility.datautility.DataWriter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles every encounter in The Fractured Archive. The raid announces the start and end of each boss in chat
 * ("Challenge started: X." / "Challenge complete: X. Duration: a. Total: b."), so a single handler follows whichever
 * boss is currently being fought, the same way the Colosseum handler follows waves.
 */
@Slf4j
public class TFAHandler extends RoomHandler
{
    private static final String TIME = "(\\d+(?::\\d{2}){1,2}(?:\\.\\d{1,3})?)";
    private static final Pattern CHALLENGE_STARTED = Pattern.compile("^Challenge started: (.+?)\\.?$");
    private static final Pattern CHALLENGE_COMPLETE = Pattern.compile("^Challenge complete: (.+?)\\. Duration: " + TIME + "\\.? Total: " + TIME + ".*$");
    private static final Pattern CHALLENGE_COMPLETE_NO_TIME = Pattern.compile("^Challenge complete: ([^.]+)\\..*$");
    private static final Pattern RAID_COMPLETE = Pattern.compile("^The Fractured Archive: Total completion time: " + TIME + ".*$");

    public static final List<RaidRoom> ROOMS = RaidRoom.getRaidRoomsForRaidType(RaidType.TFA);

    /**
     * The boss currently being fought, or the last one fought while between rooms.
     */
    @Getter
    private RaidRoom currentBoss = RaidRoom.ARCHIVE_GUARDIAN;

    /**
     * Room the player is physically standing in, UNKNOWN while in the connecting rooms. Fallback for chat names that fail to match.
     */
    @Setter
    private RaidRoom regionRoom = RaidRoom.UNKNOWN;

    @Getter
    private boolean completed = false;
    @Getter
    private boolean failed = false;
    @Getter
    private boolean abandoned = false;
    @Getter
    private int lastTotal = 0;
    private int ocellusPhase = 0;
    private final List<RaidRoom> finishedRooms = new ArrayList<>();

    public TFAHandler(Client client, DataWriter clog, AdvancedRaidTrackerConfig config, AdvancedRaidTrackerPlugin plugin)
    {
        super(client, clog, config, plugin);
    }

    @Override
    public String getName()
    {
        return currentBoss.name;
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    /**
     * Called when a new raid is entered
     */
    public void startRaid()
    {
        roomStartTick = -1;
        active = false;
        currentBoss = RaidRoom.ARCHIVE_GUARDIAN;
        regionRoom = RaidRoom.UNKNOWN;
        completed = false;
        failed = false;
        abandoned = false;
        lastTotal = 0;
        ocellusPhase = 0;
        finishedRooms.clear();
    }

    /**
     * Called when the raid is left for any reason, before the plugin writes the generic "left raid" line.
     */
    public void leaveRaid()
    {
        if (active)
        {
            clog.addLine(LogID.TFA_ROOM_LEFT, String.valueOf(client.getTickCount() - roomStartTick), getName());
        }
        int startTick = roomStartTick;
        super.reset(); //flushes prayer/damage/xp for an unfinished room
        roomStartTick = startTick; //the plugin still reports the last room tick after this
    }

    /**
     * Must be read before leaveRaid(), which clears the active flag
     */
    public String getStatusLine()
    {
        if (completed)
        {
            return "Duration (Completion): " + RoomUtil.time(lastTotal);
        }
        if (active)
        {
            return "Duration (" + getName() + " Wipe): " + RoomUtil.time(lastTotal + (client.getTickCount() - roomStartTick));
        }
        if (finishedRooms.isEmpty())
        {
            return "Duration (Reset): -";
        }
        return "Duration (" + getName() + " Reset): " + RoomUtil.time(lastTotal);
    }

    /**
     * Converts a time in the format the game prints (m:ss, m:ss.t or h:mm:ss) to ticks
     */
    public static int parseTicks(String time)
    {
        try
        {
            String[] parts = time.split(":");
            double seconds = 0;
            for (String part : parts)
            {
                seconds = (seconds * 60) + Double.parseDouble(part);
            }
            return (int) Math.round(seconds / 0.6);
        } catch (Exception e)
        {
            return -1;
        }
    }

    private RaidRoom resolveRoom(String bossName)
    {
        RaidRoom room = TfaIDs.getRoomFromBossName(bossName);
        if (room == RaidRoom.UNKNOWN && regionRoom != null && regionRoom.isTFA())
        {
            room = regionRoom;
        }
        return room;
    }

    private void selectLiveTab(RaidRoom room)
    {
        int index = ROOMS.indexOf(room);
        if (plugin.liveFrame != null && index >= 0 && index < plugin.liveFrame.tabbedPane.getTabCount())
        {
            plugin.liveFrame.tabbedPane.setSelectedIndex(index);
        }
    }

    private void roomStarted(RaidRoom room)
    {
        if (active && room == currentBoss)
        {
            return;
        }
        currentBoss = room;
        roomStartTick = client.getTickCount();
        active = true;
        ocellusPhase = (room == RaidRoom.OCELLUS) ? 1 : 0;
        clog.addLine(LogID.TFA_ROOM_STARTED, String.valueOf(roomStartTick), room.name);
        selectLiveTab(room);
    }

    private void roomFinished(RaidRoom room, String durationString, String totalString)
    {
        boolean startObserved = active && room == currentBoss && roomStartTick > 0;
        int observedDuration = client.getTickCount() - roomStartTick;
        int duration = (durationString == null) ? -1 : parseTicks(durationString);
        //Without precise timing enabled the game only reports whole seconds, in which case our own tick count is the better number
        if (startObserved && (duration < 0 || !durationString.contains(".")))
        {
            duration = observedDuration;
        }
        if (duration < 0)
        {
            return;
        }
        if (!startObserved)
        {
            currentBoss = room;
            roomStartTick = client.getTickCount() - duration;
        }
        int total = (totalString == null) ? -1 : parseTicks(totalString);
        if (total > 0)
        {
            lastTotal = total;
        } else
        {
            lastTotal += duration;
        }
        active = false;
        if (!finishedRooms.contains(room))
        {
            finishedRooms.add(room);
        }
        clog.addLine(LogID.TFA_ROOM_FINISHED, String.valueOf(duration), String.valueOf(total), room.name);
        if (plugin.liveFrame != null)
        {
            plugin.liveFrame.setRoomFinished(getName(), duration);
        }
        plugin.lastSplits += room.name + ": " + RoomUtil.time(lastTotal) + " (+" + RoomUtil.time(duration) + ")\n";
        plugin.currentDurationSum += duration;
        if (room == RaidRoom.OCELLUS)
        {
            completed = true;
        }
    }

    @Override
    public void updateChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }
        String message = Text.removeTags(event.getMessage()).replace(' ', ' ').trim();
        if (message.startsWith(TfaIDs.CHALLENGE_STARTED_MESSAGE))
        {
            Matcher matcher = CHALLENGE_STARTED.matcher(message);
            if (matcher.matches())
            {
                RaidRoom room = resolveRoom(matcher.group(1));
                if (room.isTFA())
                {
                    roomStarted(room);
                } else
                {
                    log.info("Unrecognised Fractured Archive challenge: " + message);
                }
            }
        } else if (message.startsWith(TfaIDs.CHALLENGE_COMPLETE_MESSAGE))
        {
            Matcher matcher = CHALLENGE_COMPLETE.matcher(message);
            String name;
            String duration = null;
            String total = null;
            if (matcher.matches())
            {
                name = matcher.group(1);
                duration = matcher.group(2);
                total = matcher.group(3);
            } else
            {
                Matcher fallback = CHALLENGE_COMPLETE_NO_TIME.matcher(message);
                if (!fallback.matches())
                {
                    return;
                }
                name = fallback.group(1);
            }
            RaidRoom room = resolveRoom(name);
            if (room.isTFA())
            {
                roomFinished(room, duration, total);
            } else
            {
                log.info("Unrecognised Fractured Archive challenge: " + message);
            }
        } else if (message.startsWith(TfaIDs.RAID_COMPLETE_MESSAGE))
        {
            Matcher matcher = RAID_COMPLETE.matcher(message);
            if (matcher.matches())
            {
                int total = parseTicks(matcher.group(1));
                if (total > 0)
                {
                    lastTotal = total;
                }
                completed = true;
                clog.addLine(LogID.TFA_RAID_COMPLETED, String.valueOf(total));
            }
        } else if (message.equals(TfaIDs.RAID_FAILED_MESSAGE))
        {
            failed = true;
            clog.addLine(LogID.TFA_RAID_FAILED);
        } else if (message.equals(TfaIDs.RAID_ABANDONED_MESSAGE))
        {
            abandoned = true;
            clog.addLine(LogID.TFA_RAID_ABANDONED);
        }
    }

    private void addLiveLine(int roomTick, String description)
    {
        if (plugin.liveFrame != null)
        {
            plugin.liveFrame.addLine(getName(), roomTick, description);
        }
    }

    private void checkOcellusPhase(int npcId)
    {
        if (!active || currentBoss != RaidRoom.OCELLUS)
        {
            return;
        }
        int roomTick = client.getTickCount() - roomStartTick;
        if (npcId == TfaIDs.OCELLUS_P2 && ocellusPhase == 1)
        {
            ocellusPhase = 2;
            clog.addLine(LogID.TFA_OCELLUS_P2_STARTED, roomTick);
            addLiveLine(roomTick, "P2");
        } else if (npcId == TfaIDs.OCELLUS_P3 && ocellusPhase == 2)
        {
            ocellusPhase = 3;
            clog.addLine(LogID.TFA_OCELLUS_P3_STARTED, roomTick);
            addLiveLine(roomTick, "P3");
        }
    }

    @Override
    public void updateNpcSpawned(NpcSpawned event)
    {
        checkOcellusPhase(event.getNpc().getId());
    }

    @Override
    public void handleNPCChanged(int id)
    {
        checkOcellusPhase(id);
    }
}
