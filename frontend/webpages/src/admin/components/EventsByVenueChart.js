import React from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

function EventsByVenueChart({ data }) {
  const chartData = data.map((v) => ({
    name: v.venue.length > 15 ? v.venue.substring(0, 15) + '...' : v.venue,
    count: v.count,
  }));

  return (
    <Card sx={{ backgroundColor: 'background.paper', height: '100%' }}>
      <CardContent>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
          Events by Venue
        </Typography>
        <ResponsiveContainer width="100%" height={320}>
          <BarChart data={chartData}>
            <XAxis dataKey="name" stroke="#b0b0b0" tick={{ fontSize: 12 }} />
            <YAxis stroke="#b0b0b0" allowDecimals={false} />
            <Tooltip contentStyle={{ backgroundColor: '#16213e', border: 'none', color: '#fff' }} />
            <Bar dataKey="count" fill="#0f3460" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

export default EventsByVenueChart;
