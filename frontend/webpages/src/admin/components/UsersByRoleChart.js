import React from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const COLORS = { USER: '#2196f3', ADMIN: '#e94560' };

function UsersByRoleChart({ data }) {
  const chartData = Object.entries(data).map(([name, value]) => ({ name, value }));

  return (
    <Card sx={{ backgroundColor: 'background.paper', height: '100%', width: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
          Users by Role
        </Typography>
        <div style={{ width: '100%', height: 300 }}>
          <ResponsiveContainer>
            <PieChart margin={{ top: 10, right: 10, bottom: 10, left: 10 }}>
              <Pie data={chartData} dataKey="value" nameKey="name" cx="50%" cy="45%" outerRadius="70%" label={false}>
                {chartData.map((entry) => (
                  <Cell key={entry.name} fill={COLORS[entry.name] || '#8884d8'} />
                ))}
              </Pie>
              <Tooltip />
              <Legend wrapperStyle={{ paddingTop: 10 }} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}

export default UsersByRoleChart;
