package com.advancedraidtracker.utility.datautility.datapoints.tfa;

import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Statistics that need more than one Fractured Archive run: dry streaks and per-party-size personal bests.
 */
public class TfaStatistics
{
    /**
     * @param streak            chests opened since the last unique, counting up to and including the chosen raid
     * @param receivedUnique    true if the chosen raid's own chest held a unique, in which case streak is how dry it was before that
     * @param chestsRecorded    total chests with recorded loot among the raids considered
     * @param uniquesRecorded   total uniques among those chests
     */
    public static class DryStreak
    {
        public final int streak;
        public final boolean receivedUnique;
        public final int chestsRecorded;
        public final int uniquesRecorded;

        DryStreak(int streak, boolean receivedUnique, int chestsRecorded, int uniquesRecorded)
        {
            this.streak = streak;
            this.receivedUnique = receivedUnique;
            this.chestsRecorded = chestsRecorded;
            this.uniquesRecorded = uniquesRecorded;
        }
    }

    private static boolean hasChest(Raid raid)
    {
        return raid.get(DataPoint.TFA_LOOT_VALUE) > 0;
    }

    /**
     * Raids without recorded loot (wipes, or raids from before loot tracking) are ignored, they neither extend nor break a streak.
     */
    public static DryStreak getDryStreak(Tfa chosen, List<? extends Raid> all)
    {
        List<Tfa> raids = new ArrayList<>();
        for (Raid raid : all)
        {
            if (raid instanceof Tfa && hasChest(raid) && raid.getDate().getTime() <= chosen.getDate().getTime())
            {
                raids.add((Tfa) raid);
            }
        }
        raids.sort(Comparator.comparing(raid -> raid.getDate().getTime()));
        int streak = 0;
        int uniques = 0;
        boolean chosenUnique = false;
        for (Tfa raid : raids)
        {
            int raidUniques = raid.get(DataPoint.TFA_UNIQUES);
            uniques += Math.max(raidUniques, 0);
            boolean isChosen = raid.getDate().getTime() == chosen.getDate().getTime();
            if (raidUniques > 0)
            {
                if (isChosen)
                {
                    chosenUnique = true;
                    break;
                }
                streak = 0;
            } else
            {
                streak++;
            }
        }
        return new DryStreak(streak, chosenUnique, raids.size(), uniques);
    }

    /**
     * @return fastest completed time in ticks among raids with the same party size, or -1 when there is none
     */
    public static int getBestTimeForScale(int scale, List<? extends Raid> all)
    {
        int best = -1;
        for (Raid raid : all)
        {
            if (raid instanceof Tfa && raid.isCompleted() && raid.getScale() == scale)
            {
                int time = raid.get(DataPoint.OVERALL_TIME);
                if (time > 0 && (best < 0 || time < best))
                {
                    best = time;
                }
            }
        }
        return best;
    }
}
