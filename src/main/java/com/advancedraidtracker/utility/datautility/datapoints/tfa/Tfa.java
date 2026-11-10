package com.advancedraidtracker.utility.datautility.datapoints.tfa;

import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.constants.RaidType;
import com.advancedraidtracker.utility.RoomUtil;
import com.advancedraidtracker.utility.datautility.datapoints.LogEntry;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Text;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.advancedraidtracker.utility.datautility.DataPoint.*;

/**
 * A run of The Fractured Archive: Archive Guardian, one boss from each of three pairs, then Ocellus.
 */
@Slf4j
public class Tfa extends Raid
{
    /**
     * Amount of bosses in a full run
     */
    public static final int ROOMS_PER_RAID = 5;

    private final List<RaidRoom> roomsFought = new ArrayList<>();
    private final Map<RaidRoom, Integer> unfinishedRoomTicks = new HashMap<>();
    private RaidRoom activeRoom = null;
    private boolean failed = false;
    private boolean abandoned = false;
    private int lastTotal = 0;

    public Tfa(Path filepath, List<LogEntry> raidData)
    {
        super(filepath, raidData);
    }

    /**
     * @return the bosses that were started this raid, in the order they were fought
     */
    public List<RaidRoom> getRoomsFought()
    {
        return Collections.unmodifiableList(roomsFought);
    }

    /**
     * @return room time if the room was finished, otherwise the ticks spent in it before the raid ended
     */
    public int getChartEndTick(RaidRoom room)
    {
        int time = get(room.name + " Time");
        return (time > 0) ? time : unfinishedRoomTicks.getOrDefault(room, 0);
    }

    private void roomStarted(RaidRoom room)
    {
        currentRoom = room.name;
        roomStarts.add(currentRoom);
        wasReset = false;
        activeRoom = room;
        if (!roomsFought.contains(room))
        {
            roomsFought.add(room);
        }
    }

    private void roomFinished(RaidRoom room, int duration, int total)
    {
        if (!roomsFought.contains(room))
        {
            roomsFought.add(room);
        }
        currentRoom = room.name;
        lastRoom = room.name;
        wasReset = true;
        activeRoom = null;
        if (duration > 0)
        {
            data.set(room.name + " Time", duration);
        }
        lastTotal = (total > 0) ? total : lastTotal + Math.max(duration, 0);
        data.set(room.name + " Split", lastTotal);
        if (room == RaidRoom.OCELLUS)
        {
            completed = true;
            if (get(OCELLUS_P3_SPLIT) > 0 && duration > get(OCELLUS_P3_SPLIT))
            {
                data.set(OCELLUS_P3_DURATION, duration - get(OCELLUS_P3_SPLIT));
            }
            setOverallTime(lastTotal);
        }
    }

    private void setOverallTime(int total)
    {
        if (total > 0)
        {
            data.set(OVERALL_TIME, total);
            data.set(TIME_OUTSIDE_ROOMS, Math.max(total - getChallengeTime(), 0));
        }
    }

    @Override
    protected boolean parseLogEntry(LogEntry entry)
    {
        try
        {
            switch (entry.logEntry)
            {
                case TFA_ROOM_STARTED:
                {
                    RaidRoom room = RaidRoom.getRoom(entry.getValue("Room"));
                    if (room.isTFA())
                    {
                        roomStarted(room);
                    }
                    break;
                }
                case TFA_ROOM_FINISHED:
                {
                    RaidRoom room = RaidRoom.getRoom(entry.getValue("Room"));
                    if (room.isTFA())
                    {
                        roomFinished(room, entry.getValueAsInt("Duration"), entry.getValueAsInt("Total"));
                    }
                    break;
                }
                case TFA_ROOM_LEFT:
                {
                    RaidRoom room = RaidRoom.getRoom(entry.getValue("Room"));
                    if (room.isTFA())
                    {
                        unfinishedRoomTicks.put(room, entry.getValueAsInt("Duration"));
                    }
                    break;
                }
                case TFA_RAID_COMPLETED:
                    completed = true;
                    setOverallTime(entry.getValueAsInt("Total"));
                    break;
                case TFA_RAID_FAILED:
                    failed = true;
                    break;
                case TFA_RAID_ABANDONED:
                    abandoned = true;
                    break;
                default:
                    break;
            }
        } catch (Exception e)
        {
            log.info("Could not parse Fractured Archive entry: " + String.join(",", entry.lines));
        }
        return super.parseLogEntry(entry);
    }

    @Override
    public String getRoomStatus()
    {
        if (completed)
        {
            return green + "Completion";
        }
        if (activeRoom != null)
        {
            return abandoned ? orange + activeRoom.name + " Reset" : red + activeRoom.name + " Wipe";
        }
        if (roomsFought.isEmpty())
        {
            return (failed ? red : orange) + "Guardian " + (failed ? "Wipe" : "Reset");
        }
        if (failed && !abandoned)
        {
            return red + lastRoom + " Wipe";
        }
        return orange + lastRoom + " Reset";
    }

    @Override
    public RaidType getRaidType()
    {
        return RaidType.TFA;
    }

    @Override
    public boolean isAccurate()
    {
        return true;
    }

    @Override
    public boolean getRoomAccurate(RaidRoom room)
    {
        return room.isTFA();
    }

    @Override
    public boolean getOverallTimeAccurate()
    {
        return true;
    }

    @Override
    public boolean isFullRaid()
    {
        int finished = 0;
        for (RaidRoom room : RaidRoom.getRaidRoomsForRaidType(RaidType.TFA))
        {
            if (get(room.name + " Time") > 0)
            {
                finished++;
            }
        }
        return isCompleted() && finished >= ROOMS_PER_RAID;
    }

    @Override
    public int getTimeSum()
    {
        int sum = 0;
        for (RaidRoom room : RaidRoom.getRaidRoomsForRaidType(RaidType.TFA))
        {
            sum += Math.max(get(room.name + " Time"), 0);
        }
        return sum;
    }

    @Override
    public int getChallengeTime()
    {
        return getTimeSum();
    }

    @Override
    public String getSplits()
    {
        LocalDate date = getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
        StringBuilder split = new StringBuilder(getScaleString() + ", " + formatter.format(date) + "\n");
        for (RaidRoom room : roomsFought)
        {
            int time = get(room.name + " Time");
            if (time > 0)
            {
                split.append(room.name).append(": ").append(RoomUtil.time(get(room.name + " Split"))).append(" (+").append(RoomUtil.time(time)).append(")\n");
            }
        }
        if (completed)
        {
            split.append("Duration (Completion): ").append(RoomUtil.time(get(OVERALL_TIME)));
        } else
        {
            split.append("Duration (").append(Text.removeTags(getRoomStatus())).append("): ").append(RoomUtil.time(getChallengeTime()));
        }
        return split.toString();
    }
}
