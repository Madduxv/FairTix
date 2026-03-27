import React, { useState, useEffect } from 'react';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import EventIcon from '@mui/icons-material/Event';
import UpcomingIcon from '@mui/icons-material/CalendarMonth';
import PeopleIcon from '@mui/icons-material/People';
import ChairIcon from '@mui/icons-material/Chair';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import PanToolIcon from '@mui/icons-material/PanTool';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import PercentIcon from '@mui/icons-material/Percent';
import StatsCard from '../components/StatsCard';
import SeatStatusChart from '../components/SeatStatusChart';
import HoldStatusChart from '../components/HoldStatusChart';
import TopEventsChart from '../components/TopEventsChart';
import HoldsOverTimeChart from '../components/HoldsOverTimeChart';
import EventsByVenueChart from '../components/EventsByVenueChart';
import UsersByRoleChart from '../components/UsersByRoleChart';
import api from '../../api/client';

function AdminDashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchAnalytics() {
      try {
        const result = await api.get('/api/analytics/dashboard');
        setData(result);
      } catch (err) {
        console.error('Failed to fetch analytics:', err);
        setError('Failed to load analytics data.');
      } finally {
        setLoading(false);
      }
    }
    fetchAnalytics();
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>;
  }

  const { overview } = data;

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
        Dashboard
      </Typography>

      {/* Row 1: Overview Stats */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={4} lg={4}>
          <StatsCard title="Total Events" value={overview.totalEvents} icon={<EventIcon />} color="#e94560" />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={4}>
          <StatsCard title="Upcoming Events" value={overview.upcomingEvents} icon={<UpcomingIcon />} color="#0f3460" />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={4}>
          <StatsCard title="Total Users" value={overview.totalUsers} icon={<PeopleIcon />} color="#533483" />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={4}>
          <StatsCard title="Total Seats" value={overview.totalSeats} icon={<ChairIcon />} color="#2196f3" />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={4}>
          <StatsCard title="Booked Seats" value={overview.bookedSeats} icon={<ConfirmationNumberIcon />} color="#4caf50" />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={4}>
          <StatsCard title="Active Holds" value={overview.activeHolds} icon={<PanToolIcon />} color="#ff9800" />
        </Grid>
        <Grid item xs={12} sm={6} md={4} lg={4}>
          <StatsCard title="Sold Seats" value={overview.soldSeats} icon={<ShoppingCartIcon />} color="#2196f3" />
        </Grid>
      </Grid>

      {/* Row 2: Seat & Hold Status Pies */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={6}>
          <SeatStatusChart data={data.seatsByStatus} />
        </Grid>
        <Grid item xs={12} md={6}>
          <HoldStatusChart data={data.holdsByStatus} />
        </Grid>
      </Grid>

      {/* Row 3: Top Events */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12}>
          <TopEventsChart data={data.topEventsByBookings} />
        </Grid>
      </Grid>

      {/* Row 4: Holds Over Time & Events by Venue */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={7}>
          <HoldsOverTimeChart data={data.holdsPerDay} />
        </Grid>
        <Grid item xs={12} md={5}>
          <EventsByVenueChart data={data.eventsByVenue} />
        </Grid>
      </Grid>

      {/* Row 5: Confirmation Rate & Users by Role */}
      <Grid container spacing={3}>
        <Grid item xs={12} sm={6} md={4}>
          <StatsCard
            title="Hold Confirmation Rate"
            value={`${data.holdConfirmationRate}%`}
            icon={<PercentIcon />}
            color="#4caf50"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={8}>
          <UsersByRoleChart data={data.usersByRole} />
        </Grid>
      </Grid>
    </Box>
  );
}

export default AdminDashboard;
