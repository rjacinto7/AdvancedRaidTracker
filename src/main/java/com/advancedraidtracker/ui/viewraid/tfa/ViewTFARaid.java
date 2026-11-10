package com.advancedraidtracker.ui.viewraid.tfa;

import com.advancedraidtracker.AdvancedRaidTrackerConfig;
import com.advancedraidtracker.constants.RaidRoom;
import com.advancedraidtracker.ui.BaseFrame;
import com.advancedraidtracker.utility.RoomUtil;
import com.advancedraidtracker.utility.datautility.DataPoint;
import com.advancedraidtracker.utility.datautility.datapoints.tfa.Tfa;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.List;

import static com.advancedraidtracker.utility.UISwingUtility.*;

public class ViewTFARaid extends BaseFrame
{
    private final Tfa tfaData;
    private final String red = "<html><font color='#FF0000'>";
    private final String green = "<html><font color='#44AF33'>";
    private final String blue = "<html><font color='#6666DD'>";

    public ViewTFARaid(Tfa tfaData, AdvancedRaidTrackerConfig config)
    {
        this.tfaData = tfaData;
        setTitle("View Raid");
        setPreferredSize(new Dimension(800, 560));
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel topContainer = getThemedPanel();
        topContainer.setLayout(new GridLayout(1, 2));
        topContainer.setPreferredSize(new Dimension(800, 210));
        topContainer.add(getSummaryPanel());
        topContainer.add(getSplitsPanel());

        //Five bosses per run, laid out in fight order
        JPanel bottomContainer = getThemedPanel();
        bottomContainer.setPreferredSize(new Dimension(800, 350));
        bottomContainer.setLayout(new GridLayout(2, 3));
        for (RaidRoom room : tfaData.getRoomsFought())
        {
            bottomContainer.add(getRoomPanel(room));
        }
        for (int i = tfaData.getRoomsFought().size(); i < Tfa.ROOMS_PER_RAID + 1; i++)
        {
            bottomContainer.add(getThemedPanel());
        }

        add(topContainer);
        add(bottomContainer);

        setResizable(false);
        pack();
    }

    private int sumOverRooms(DataPoint point)
    {
        int sum = 0;
        for (RaidRoom room : tfaData.getRoomsFought())
        {
            sum += Math.max(tfaData.get(point, room), 0);
        }
        return sum;
    }

    private JPanel getSummaryPanel()
    {
        JPanel panel = getTitledPanel("Summary");
        panel.setLayout(new GridLayout(1, 2));

        JPanel leftPanel = getThemedPanel();
        JPanel rightPanel = getThemedPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        Calendar cal = Calendar.getInstance();
        cal.setTime(tfaData.getDate());
        String dateString = (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "-" + cal.get(Calendar.YEAR);

        leftPanel.add(getThemedLabel("Date: " + dateString));
        leftPanel.add(getThemedLabel("Scale: " + tfaData.getScaleString()));
        leftPanel.add(getThemedLabel("<html>Raid Status: " + tfaData.getRoomStatus().replace("<html>", "")));
        leftPanel.add(getThemedLabel("Overall Time: " + RoomUtil.time(tfaData.get(DataPoint.OVERALL_TIME))));
        leftPanel.add(getThemedLabel("Time In Fights: " + RoomUtil.time(tfaData.getChallengeTime())));
        leftPanel.add(getThemedLabel("Time Between Fights: " + RoomUtil.time(tfaData.get(DataPoint.TIME_OUTSIDE_ROOMS))));
        leftPanel.add(getThemedLabel());
        leftPanel.add(getThemedLabel("Deaths: " + sumOverRooms(DataPoint.DEATHS)));
        leftPanel.add(getThemedLabel("<html>Damage Dealt: " + green.replace("<html>", "") + sumOverRooms(DataPoint.DAMAGE_DEALT)));
        leftPanel.add(getThemedLabel("<html>Damage Received: " + red.replace("<html>", "") + sumOverRooms(DataPoint.DAMAGE_RECEIVED)));
        leftPanel.add(getThemedLabel("<html>Prayer Used: " + blue.replace("<html>", "") + sumOverRooms(DataPoint.PRAYER_USED)));

        rightPanel.add(getThemedLabel("Players: "));
        for (String player : tfaData.getPlayers())
        {
            rightPanel.add(getThemedLabel("    " + player));
        }

        panel.add(leftPanel);
        panel.add(rightPanel);
        return panel;
    }

    private JPanel getSplitsPanel()
    {
        JPanel panel = getTitledPanel("Splits");
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        List<RaidRoom> rooms = tfaData.getRoomsFought();
        if (rooms.isEmpty())
        {
            panel.add(getThemedLabel("No bosses were started"));
        }
        for (RaidRoom room : rooms)
        {
            int time = tfaData.get(room.name + " Time");
            if (time > 0)
            {
                panel.add(getSplitPanel(room.name + ": ", RoomUtil.time(tfaData.get(room.name + " Split")) + " (+" + RoomUtil.time(time) + ")"));
            } else
            {
                panel.add(getSplitPanel(room.name + ": ", red + "Not completed (" + RoomUtil.time(tfaData.getChartEndTick(room)) + ")"));
            }
        }
        return panel;
    }

    private JPanel getRoomPanel(RaidRoom room)
    {
        int time = tfaData.get(room.name + " Time");
        String title = (time > 0) ? green + room.name + " - " + RoomUtil.time(time) : red + room.name + " - Not completed";
        JPanel panel = getTitledPanel(title);
        panel.setLayout(new GridLayout(0, 2));

        panel.add(getThemedLabel("Room Time: "));
        panel.add(getThemedLabel((time > 0) ? green + RoomUtil.time(time) : RoomUtil.time(tfaData.getChartEndTick(room))));

        panel.add(getThemedLabel("Raid Split: "));
        panel.add(getThemedLabel(RoomUtil.time(tfaData.get(room.name + " Split"))));

        if (room == RaidRoom.OCELLUS)
        {
            panel.add(getThemedLabel("P1 Time: "));
            panel.add(getThemedLabel(RoomUtil.time(tfaData.get(DataPoint.OCELLUS_P1_DURATION))));

            panel.add(getThemedLabel("P2 Time: "));
            panel.add(getThemedLabel(RoomUtil.time(tfaData.get(DataPoint.OCELLUS_P2_DURATION))));

            panel.add(getThemedLabel("P3 Time: "));
            panel.add(getThemedLabel(RoomUtil.time(tfaData.get(DataPoint.OCELLUS_P3_DURATION))));
        }

        panel.add(getThemedLabel("Deaths: "));
        panel.add(getThemedLabel(String.valueOf(tfaData.get(DataPoint.DEATHS, room))));

        panel.add(getThemedLabel("Damage Dealt: "));
        panel.add(getThemedLabel(green + tfaData.get(DataPoint.DAMAGE_DEALT, room)));

        panel.add(getThemedLabel("Damage Received: "));
        panel.add(getThemedLabel(red + tfaData.get(DataPoint.DAMAGE_RECEIVED, room)));

        panel.add(getThemedLabel("Prayer Used: "));
        panel.add(getThemedLabel(blue + tfaData.get(DataPoint.PRAYER_USED, room)));

        return panel;
    }
}
