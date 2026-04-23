import React from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';

function RevenueOverTimeChart({ data }) {
  const chartData = data.map((d) => ({
    date: d.date,
    amount: parseFloat(d.amount),
  }));

  const formatCurrency = (value) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);

  return (
    <Card sx={{ backgroundColor: 'background.paper', height: '100%' }}>
      <CardContent>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
          Revenue Over Time (Last 30 Days)
        </Typography>
        <ResponsiveContainer width="100%" height={320}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
            <XAxis dataKey="date" stroke="#b0b0b0" tick={{ fontSize: 11 }} angle={-35} textAnchor="end" height={60} />
            <YAxis stroke="#b0b0b0" tickFormatter={formatCurrency} width={80} />
            <Tooltip
              contentStyle={{ backgroundColor: '#16213e', border: 'none', color: '#fff' }}
              formatter={(value) => [formatCurrency(value), 'Revenue']}
            />
            <Line type="monotone" dataKey="amount" stroke="#4caf50" strokeWidth={2} dot={{ r: 3 }} />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

export default RevenueOverTimeChart;
