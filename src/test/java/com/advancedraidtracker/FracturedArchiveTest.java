package com.advancedraidtracker;

import com.advancedraidtracker.constants.LogID;
import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.constants.RaidType;
import com.advancedraidtracker.constants.TfaIDs;
import com.advancedraidtracker.rooms.tfa.TFAHandler;
import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.datautility.DataReader;
import com.advancedraidtracker.utility.datautility.DataWriter;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;
import com.advancedraidtracker.utility.datautility.datapoints.tfa.Tfa;
import com.advancedraidtracker.utility.weapons.AnimationDecider;
import com.advancedraidtracker.utility.weapons.PlayerAnimation;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemID;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Drives the Fractured Archive handler with the chat messages the raid sends, then reads the log it produced back
 * through the same reader the UI uses.
 */
public class FracturedArchiveTest
{
    private int tick;
    private DataWriter clog;
    private TFAHandler handler;
    private AdvancedRaidTrackerPlugin plugin;

    private static Object defaultValue(Class<?> type)
    {
        if (type == boolean.class)
        {
            return false;
        } else if (type == int.class || type == short.class || type == byte.class)
        {
            return 0;
        } else if (type == long.class)
        {
            return 0L;
        } else if (type == double.class)
        {
            return 0d;
        } else if (type == float.class)
        {
            return 0f;
        } else if (type == char.class)
        {
            return (char) 0;
        }
        return null;
    }

    @Before
    public void setUp() throws Exception
    {
        tick = 1000;
        Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class[]{Client.class},
                (proxy, method, args) -> method.getName().equals("getTickCount") ? (Object) tick : defaultValue(method.getReturnType()));
        AdvancedRaidTrackerConfig config = (AdvancedRaidTrackerConfig) Proxy.newProxyInstance(AdvancedRaidTrackerConfig.class.getClassLoader(),
                new Class[]{AdvancedRaidTrackerConfig.class}, (proxy, method, args) -> defaultValue(method.getReturnType()));
        clog = new DataWriter(config);
        clog.setRaidType(RaidType.TFA);
        plugin = new AdvancedRaidTrackerPlugin();
        handler = new TFAHandler(client, clog, config, plugin);
        handler.startRaid();
        clog.addLine(LogID.ENTERED_RAID);
        clog.addLine(LogID.PARTY_MEMBERS, "Player One", "Player Two", "", "", "", "", "", "");
    }

    private void game(String message)
    {
        chat(ChatMessageType.GAMEMESSAGE, message);
    }

    private void chat(ChatMessageType type, String message)
    {
        ChatMessage event = new ChatMessage();
        event.setType(type);
        event.setMessage(message);
        handler.updateChatMessage(event);
    }

    @SuppressWarnings("unchecked")
    private Raid readBack() throws Exception
    {
        clog.addLine(LogID.LEFT_TOB, String.valueOf(tick - handler.roomStartTick), handler.getName());
        Field buffer = DataWriter.class.getDeclaredField("currentBuffer");
        buffer.setAccessible(true);
        List<String> lines = (List<String>) buffer.get(clog);
        Path directory = Files.createTempDirectory("art-tfa-test");
        Path file = directory.resolve("TFAdata1.log");
        Files.write(file, lines);
        try
        {
            return DataReader.getRaid(file);
        } finally
        {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void timesAreConvertedToTicks()
    {
        assertEquals(150, TFAHandler.parseTicks("1:30"));
        assertEquals(151, TFAHandler.parseTicks("1:30.60"));
        assertEquals(1, TFAHandler.parseTicks("0:00.60"));
        assertEquals(6100, TFAHandler.parseTicks("1:01:00"));
        assertEquals(-1, TFAHandler.parseTicks("soon"));
    }

    @Test
    public void bossNamesFromChatResolveToRooms()
    {
        assertEquals(RaidRoom.ARCHIVE_GUARDIAN, TfaIDs.getRoomFromBossName("Archive Guardian"));
        assertEquals(RaidRoom.BORK, TfaIDs.getRoomFromBossName("Bork and the Smorks"));
        assertEquals(RaidRoom.BORK, TfaIDs.getRoomFromBossName("Bork"));
        assertEquals(RaidRoom.DAGONHAI, TfaIDs.getRoomFromBossName("Dagon'hai Elder"));
        assertEquals(RaidRoom.DREADNOTH, TfaIDs.getRoomFromBossName("Dreadnoth the Jellied"));
        assertEquals(RaidRoom.UNKNOWN, TfaIDs.getRoomFromBossName("Verzik Vitur"));
    }

    @Test
    public void newWeaponsAreRecognisedOnTheChart()
    {
        assertEquals(PlayerAnimation.OBLIGATOR_THRUST, AnimationDecider.getWeapon("15123", "", "-1", 34715));
        assertEquals(PlayerAnimation.OBLIGATOR_THRUST, AnimationDecider.getWeapon("15131", "", "-1", 34717));
        assertEquals(PlayerAnimation.OBLIGATOR_SMASH, AnimationDecider.getWeapon("15122", "4423:", "-1", 34715));
        assertEquals(PlayerAnimation.ASCENSION_CROSSBOWS, AnimationDecider.getWeapon("15113", "", "-1", 34681));
        assertEquals(PlayerAnimation.ASCENSION_CROSSBOWS, AnimationDecider.getWeapon("15116", "", "-1", 34681));
        assertEquals(PlayerAnimation.ZORYAS_TOME, AnimationDecider.getWeapon("15146", "", "-1", 34725));
        assertEquals(PlayerAnimation.ZORYAS_TOME_SPEC, AnimationDecider.getWeapon("15140", "4432:", "-1", 34725));
        assertEquals(PlayerAnimation.CRIMSON_KISTEN, AnimationDecider.getWeapon("14253", "", "-1", 33631));
        assertEquals(PlayerAnimation.CRIMSON_KISTEN_SPEC, AnimationDecider.getWeapon("14255", "3908:", "-1", 33631));
        assertEquals(PlayerAnimation.EYE_OF_AYAK, AnimationDecider.getWeapon("12397", "", "-1", 31113));
        assertEquals(PlayerAnimation.EYE_OF_AYAK_SPEC, AnimationDecider.getWeapon("12394", "", "-1", 31113));
        assertEquals(PlayerAnimation.HALLOWFELL, AnimationDecider.getWeapon("14469", "", "-1", 34027));
        assertEquals(PlayerAnimation.HALLOWED_FLAIL, AnimationDecider.getWeapon("14249", "", "-1", 33718));
        assertEquals(PlayerAnimation.INFERNAL_TECPATL_SPEC, AnimationDecider.getWeapon("13762", "", "-1", 33243));
        //Weapons that share generic animations are told apart by the equipped item
        assertEquals(PlayerAnimation.EMBERLIGHT, AnimationDecider.getWeapon("390", "", "-1", 29589));
        assertEquals(PlayerAnimation.ARKAN_BLADE, AnimationDecider.getWeapon("390", "", "-1", 30955));
        assertEquals(PlayerAnimation.BLADE_OF_SAELDOR, AnimationDecider.getWeapon("390", "", "-1", ItemID.BLADE_OF_SAELDOR));
        assertEquals(PlayerAnimation.BURNING_CLAW_SCRATCH, AnimationDecider.getWeapon("393", "", "-1", 29577));
        assertEquals(PlayerAnimation.BURNING_CLAW_SPEC, AnimationDecider.getWeapon("11140", "", "-1", 29577));
        assertEquals(PlayerAnimation.CLAW_SCRATCH, AnimationDecider.getWeapon("393", "", "-1", ItemID.DRAGON_CLAWS));
        assertEquals(PlayerAnimation.SCORCHING_BOW, AnimationDecider.getWeapon("426", "", "-1", 29591));
        assertEquals(PlayerAnimation.NOXIOUS_HALBERD, AnimationDecider.getWeapon("440", "", "-1", 29796));
        assertEquals(PlayerAnimation.CHALLY_WHACK, AnimationDecider.getWeapon("440", "", "-1", ItemID.CRYSTAL_HALBERD));
        assertEquals(PlayerAnimation.LITHIC_SCEPTRE, AnimationDecider.getWeapon("1167", "", "-1", 33255));
        assertEquals(PlayerAnimation.SANG, AnimationDecider.getWeapon("1167", "", "-1", ItemID.SANGUINESTI_STAFF));
        assertEquals(PlayerAnimation.KINGS_BARRAGE, AnimationDecider.getWeapon("7552", "", "-1", 33251));
        //Existing weapons still resolve to themselves
        assertEquals(PlayerAnimation.SCYTHE, AnimationDecider.getWeapon("8056", "", "-1", ItemID.SCYTHE_OF_VITUR));
        assertEquals(PlayerAnimation.TBOW, AnimationDecider.getWeapon("426", "", "-1", ItemID.TWISTED_BOW));
        assertEquals(PlayerAnimation.DRAGON_CROSSBOW, AnimationDecider.getWeapon("7552", "", "-1", ItemID.DRAGON_CROSSBOW));
    }

    @Test
    public void existingRoomOrdinalsAreUnchanged()
    {
        //Theatre of Blood logs store the room as an ordinal
        assertEquals(0, RaidRoom.MAIDEN.ordinal());
        assertEquals(5, RaidRoom.VERZIK.ordinal());
        assertEquals(8, RaidRoom.getRaidRoomsForRaidType(RaidType.TFA).size());
        assertEquals(RaidType.TFA, DataPoint.getValue("Guardian Time").getRaidType());
        assertEquals(RaidType.TFA, DataPoint.getValue("Dagon'hai Split").getRaidType());
        assertEquals(RaidType.TOB, DataPoint.MAIDEN_TIME.getRaidType());
    }

    @Test
    public void completedRaidIsParsedFromChat() throws Exception
    {
        game("Challenge started: Archive Guardian.");
        assertTrue(handler.isActive());
        assertEquals("Guardian", handler.getName());
        tick += 150;
        //Whole seconds only: the handler's own tick count is used for the room, the game's value for the raid total
        game("Challenge complete: Archive Guardian. Duration: <col=ef1020>1:30</col>. Total: <col=ef1020>1:45</col>");
        assertFalse(handler.isActive());

        //Somebody typing the message into public chat must not count
        chat(ChatMessageType.PUBLICCHAT, "Challenge started: Ocellus.");
        assertFalse(handler.isActive());

        tick += 100;
        game("Challenge started: Bork and the Smorks.");
        assertEquals("Bork", handler.getName());
        tick += 203; //precise timing wins over the observed tick count
        game("Challenge complete: Bork and the Smorks. Duration: <col=ef1020>2:00.00</col>. Total: <col=ef1020>5:15.00</col>");

        tick += 50;
        game("Challenge started: Moia.");
        tick += 300;
        game("Challenge complete: Moia. Duration: 3:00.00. Total: 9:00.00. Personal best: 2:50.20");

        tick += 50;
        game("Challenge started: Dagon'hai Elder.");
        tick += 250;
        game("Challenge complete: Dagon'hai Elder. Duration: 2:30.00. Total: 12:00.00");

        tick += 50;
        game("Challenge started: Ocellus.");
        tick += 100;
        handler.handleNPCChanged(TfaIDs.OCELLUS_P3); //out of order, ignored
        handler.handleNPCChanged(TfaIDs.OCELLUS_P2);
        handler.handleNPCChanged(TfaIDs.OCELLUS_P2);
        tick += 200;
        handler.handleNPCChanged(TfaIDs.OCELLUS_P3);
        tick += 300;
        game("Challenge complete: Ocellus. Duration: 6:00.00. Total: 19:00.00");
        game("The Fractured Archive: Total completion time: <col=ef1020>19:00.00</col>. Personal best: 18:00.00");
        assertTrue(handler.isCompleted());
        assertEquals("Duration (Completion): 19:00.0", handler.getStatusLine());
        handler.leaveRaid();

        Raid raid = readBack();
        assertTrue(raid instanceof Tfa);
        Tfa tfa = (Tfa) raid;
        assertEquals(RaidType.TFA, tfa.getRaidType());
        assertEquals(2, tfa.getScale());
        assertTrue(tfa.isCompleted());
        assertTrue(tfa.isFullRaid());
        assertTrue(tfa.getRoomStatus().endsWith("Completion"));
        assertEquals(Arrays.asList("Guardian", "Bork", "Moia", "Dagon'hai", "Ocellus"),
                tfa.getRoomsFought().stream().map(room -> room.name).collect(Collectors.toList()));

        assertEquals(150, tfa.get(DataPoint.GUARDIAN_TIME));
        assertEquals(175, tfa.get(DataPoint.GUARDIAN_SPLIT));
        assertEquals(200, tfa.get(DataPoint.BORK_TIME));
        assertEquals(525, tfa.get(DataPoint.BORK_SPLIT));
        assertEquals(300, tfa.get(DataPoint.MOIA_TIME));
        assertEquals(250, (int) tfa.get("Dagon'hai Time"));
        assertEquals(600, tfa.get(DataPoint.OCELLUS_TIME));
        assertEquals(0, tfa.get(DataPoint.EZEKIEL_TIME));

        assertEquals(100, tfa.get(DataPoint.OCELLUS_P1_DURATION));
        assertEquals(100, tfa.get(DataPoint.OCELLUS_P2_SPLIT));
        assertEquals(200, tfa.get(DataPoint.OCELLUS_P2_DURATION));
        assertEquals(300, tfa.get(DataPoint.OCELLUS_P3_SPLIT));
        assertEquals(300, tfa.get(DataPoint.OCELLUS_P3_DURATION));

        assertEquals(1500, tfa.getChallengeTime());
        assertEquals(1900, tfa.get(DataPoint.OVERALL_TIME));
        assertEquals(400, tfa.get(DataPoint.TIME_OUTSIDE_ROOMS));
        assertEquals(600, tfa.getChartEndTick(RaidRoom.OCELLUS));

        assertTrue(tfa.getTimeAccurate(DataPoint.BORK_TIME));
        assertFalse(tfa.getTimeAccurate(DataPoint.MAIDEN_TIME));
        assertTrue(plugin.lastSplits.contains("Bork: 5:15.0 (+2:00.0)"));
        assertTrue(tfa.getSplits().contains("Duration (Completion): 19:00.0"));
    }

    @Test
    public void wipeMidRoomIsNotACompletion() throws Exception
    {
        game("Challenge started: Archive Guardian.");
        tick += 120;
        game("Challenge complete: Archive Guardian. Duration: 1:12.00. Total: 1:20.40");
        tick += 40;
        game("Challenge started: Ezekiel.");
        tick += 80;
        game("You failed to survive the Fractured Archive.");
        assertTrue(handler.isFailed());
        assertTrue(handler.getStatusLine().startsWith("Duration (Ezekiel Wipe)"));
        handler.leaveRaid();

        Tfa tfa = (Tfa) readBack();
        assertFalse(tfa.isCompleted());
        assertFalse(tfa.isFullRaid());
        assertTrue(tfa.getRoomStatus().endsWith("Ezekiel Wipe"));
        assertEquals(120, tfa.get(DataPoint.GUARDIAN_TIME));
        assertEquals(134, tfa.get(DataPoint.GUARDIAN_SPLIT));
        assertEquals(0, tfa.get(DataPoint.EZEKIEL_TIME));
        assertEquals(80, tfa.getChartEndTick(RaidRoom.EZEKIEL));
        assertEquals(Arrays.asList(RaidRoom.ARCHIVE_GUARDIAN, RaidRoom.EZEKIEL), tfa.getRoomsFought());
    }

    @Test
    public void leavingBetweenRoomsIsAReset() throws Exception
    {
        //Joined late: the start message was never seen, so the room time has to come from the completion message
        tick += 500;
        game("Challenge complete: Archive Guardian. Duration: 1:30. Total: 1:40");
        game("You abandon the raid and leave the Fractured Archive.");
        assertTrue(handler.isAbandoned());
        handler.leaveRaid();

        Tfa tfa = (Tfa) readBack();
        assertFalse(tfa.isCompleted());
        assertTrue(tfa.getRoomStatus().endsWith("Guardian Reset"));
        assertEquals(150, tfa.get(DataPoint.GUARDIAN_TIME));
    }
}
