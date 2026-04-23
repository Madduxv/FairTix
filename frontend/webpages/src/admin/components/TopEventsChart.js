import React from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import { BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer } from 'recharts';

function TopEventsChart({ data }) {
  const chartData = data.map((e) => ({
    name: e.eventTitle.length > 20 ? e.eventTitle.substring(0, 20) + '...' : e.eventTitle,
    Available: e.available,
    Held: e.held,
    Booked: e.booked,
    Sold: e.sold,
  }));

  const barHeight = Math.max(380, chartData.length * 50 + 80);

  return (
    <Card sx={{ backgroundColor: 'background.paper', height: '100%', width: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
          Top Events by Bookings
        </Typography>
        <div style={{ width: '100%', height: barHeight }}>
          <ResponsiveContainer>
            <BarChart data={chartData} layout="vertical" margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <XAxis type="number" stroke="#b0b0b0" allowDecimals={false} />
              <YAxis type="category" dataKey="name" width={130} stroke="#b0b0b0" tick={{ fontSize: 12 }} />
              <Tooltip contentStyle={{ backgroundColor: '#16213e', border: 'none', color: '#fff' }} />
              <Legend />
              <Bar dataKey="Sold" fill="#2196f3" stackId="a" />
              <Bar dataKey="Booked" fill="#e94560" stackId="a" />
              <Bar dataKey="Held" fill="#ff9800" stackId="a" />
              <Bar dataKey="Available" fill="#4caf50" stackId="a" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}

export default TopEventsChart;
