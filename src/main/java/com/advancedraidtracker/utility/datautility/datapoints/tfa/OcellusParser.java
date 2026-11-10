package com.advancedraidtracker.utility.datautility.datapoints.tfa;

import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.datautility.datapoints.Raid;
import com.advancedraidtracker.utility.datautility.datapoints.RoomParser;

import java.util.Map;

public class OcellusParser extends RoomParser
{
    public OcellusParser(Raid data)
    {
        super(data);
    }

    @Override
    public int getFirstPossibleNonIdleTick()
    {
        return 0;
    }

    @Override
    public Map<Integer, String> getLines()
    {
        if (data.get(DataPoint.OCELLUS_P2_SPLIT) > 0)
        {
            lines.put(data.get(DataPoint.OCELLUS_P2_SPLIT), "P2");
        }
        if (data.get(DataPoint.OCELLUS_P3_SPLIT) > 0)
        {
            lines.put(data.get(DataPoint.OCELLUS_P3_SPLIT), "P3");
        }
        if (data.get(DataPoint.OCELLUS_TIME) > 0)
        {
            lines.put(data.get(DataPoint.OCELLUS_TIME), "End");
        }
        return lines;
    }
}
