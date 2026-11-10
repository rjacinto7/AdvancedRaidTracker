package com.advancedraidtracker.constants;

import java.util.Map;

/**
 * IDs for The Fractured Archive. Values mirror net.runelite.api.gameval (NpcID / VarbitID / VarClientID / InterfaceID).
 */
public class TfaIDs
{
    //Region IDs of the instance templates. The lobby and every boss template sit next to each other on the static map,
    //so these have to be compared against the player's own (instance translated) location rather than the loaded map regions.
    public static final int TFA_LOBBY_REGION = 16470;
    public static final int ARCHIVE_GUARDIAN_REGION = 16471;
    public static final int EZEKIEL_REGION = 16214;
    public static final int BORK_REGION = 16727;
    public static final int MOIA_REGION = 16216;
    public static final int DREADNOTH_REGION = 16728;
    public static final int DAGONHAI_REGION = 16215;
    public static final int XARTH_REGION = 16726;
    public static final int OCELLUS_REGION = 16472;
    public static final int TFA_LOOT_REGION = 16474;

    public static final Map<Integer, RaidRoom> REGION_TO_ROOM = Map.of(
            ARCHIVE_GUARDIAN_REGION, RaidRoom.ARCHIVE_GUARDIAN,
            EZEKIEL_REGION, RaidRoom.EZEKIEL,
            BORK_REGION, RaidRoom.BORK,
            MOIA_REGION, RaidRoom.MOIA,
            DREADNOTH_REGION, RaidRoom.DREADNOTH,
            DAGONHAI_REGION, RaidRoom.DAGONHAI,
            XARTH_REGION, RaidRoom.XARTH,
            OCELLUS_REGION, RaidRoom.OCELLUS
    );

    //InterfaceID.TfaHud.UNIVERSE, the in-raid HUD
    public static final int TFA_HUD_WIDGET = 0x03ce_0000;

    //VarClientID.TFA_CLIENT_NAME0 .. TFA_CLIENT_NAME7
    public static final int TFA_PARTY_NAME_VARCSTR_START = 1573;
    public static final int TFA_MAX_PARTY_SIZE = 8;

    //VarbitID.TFA_CLIENT_SPECTATE_MODE: 0 = alive, 1 = dead but part of the raid, 2+ = outside spectator
    public static final int TFA_SPECTATE_MODE_VARBIT = 16405;

    public static final int OCELLUS_P1 = 16674;
    public static final int OCELLUS_P2 = 16675;
    public static final int OCELLUS_P3 = 16677;

    //ItemID.OBLIGATOR, used as the tab icon
    public static final int OBLIGATOR_ITEM = 34715;

    public static final String CHALLENGE_STARTED_MESSAGE = "Challenge started: ";
    public static final String CHALLENGE_COMPLETE_MESSAGE = "Challenge complete: ";
    public static final String RAID_COMPLETE_MESSAGE = "The Fractured Archive: Total completion time: ";
    public static final String RAID_FAILED_MESSAGE = "You failed to survive the Fractured Archive.";
    public static final String RAID_ABANDONED_MESSAGE = "You abandon the raid and leave the Fractured Archive.";

    /**
     * @param bossName name of the boss as it appears in the challenge started/complete chat messages
     * @return matching room, or UNKNOWN
     */
    public static RaidRoom getRoomFromBossName(String bossName)
    {
        String name = bossName.toLowerCase();
        if (name.contains("guardian"))
        {
            return RaidRoom.ARCHIVE_GUARDIAN;
        } else if (name.contains("ezekiel"))
        {
            return RaidRoom.EZEKIEL;
        } else if (name.contains("bork"))
        {
            return RaidRoom.BORK;
        } else if (name.contains("moia"))
        {
            return RaidRoom.MOIA;
        } else if (name.contains("dreadnoth"))
        {
            return RaidRoom.DREADNOTH;
        } else if (name.contains("dagon"))
        {
            return RaidRoom.DAGONHAI;
        } else if (name.contains("xarth"))
        {
            return RaidRoom.XARTH;
        } else if (name.contains("ocellus"))
        {
            return RaidRoom.OCELLUS;
        }
        return RaidRoom.UNKNOWN;
    }
}
