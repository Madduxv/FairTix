import React from 'react';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Avatar from '@mui/material/Avatar';

function StatsCard({ title, value, icon, color }) {
  return (
    <Card sx={{ backgroundColor: 'background.paper', height: '100%', width: '100%' }}>
      <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 1.5, p: 2, '&:last-child': { pb: 2 } }}>
        <Avatar sx={{ backgroundColor: color, width: 44, height: 44 }}>
          {icon}
        </Avatar>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            {value}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {title}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
}

export default StatsCard;
