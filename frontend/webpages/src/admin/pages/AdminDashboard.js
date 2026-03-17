import React, { useState, useEffect } from 'react';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import EventIcon from '@mui/icons-material/Event';
import UpcomingIcon from '@mui/icons-material/CalendarMonth';
import PeopleIcon from '@mui/icons-material/People';
import StatsCard from '../components/StatsCard';
import api from '../../api/client';

function AdminDashboard() {
  const [stats, setStats] = useState({ totalEvents: 0, upcomingEvents: 0, totalUsers: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchStats() {
      try {
        const [allEvents, upcoming, users] = await Promise.all([
          api.get('/api/events?size=1'),
          api.get('/api/events?upcoming=true&size=1'),
          api.get('/api/admin/users?size=1'),
        ]);
        setStats({
          totalEvents: allEvents.totalElements || 0,
          upcomingEvents: upcoming.totalElements || 0,
          totalUsers: users.totalElements || 0,
        });
      } catch (err) {
        console.error('Failed to fetch dashboard stats:', err);
      } finally {
        setLoading(false);
      }
    }
    fetchStats();
  }, []);

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
        Dashboard
      </Typography>
      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={4}>
          <StatsCard
            title="Total Events"
            value={loading ? '...' : stats.totalEvents}
            icon={<EventIcon />}
            color="#e94560"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <StatsCard
            title="Upcoming Events"
            value={loading ? '...' : stats.upcomingEvents}
            icon={<UpcomingIcon />}
            color="#0f3460"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <StatsCard
            title="Total Users"
            value={loading ? '...' : stats.totalUsers}
            icon={<PeopleIcon />}
            color="#533483"
          />
        </Grid>
      </Grid>
    </Box>
  );
}

export default AdminDashboard;
