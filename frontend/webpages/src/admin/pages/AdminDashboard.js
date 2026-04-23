import React, { useState, useEffect } from 'react';
import Box from '@mui/material/Box';
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
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import StatsCard from '../components/StatsCard';
import SeatStatusChart from '../components/SeatStatusChart';
import HoldStatusChart from '../components/HoldStatusChart';
import TopEventsChart from '../components/TopEventsChart';
import HoldsOverTimeChart from '../components/HoldsOverTimeChart';
import EventsByVenueChart from '../components/EventsByVenueChart';
import UsersByRoleChart from '../components/UsersByRoleChart';
import RevenueOverTimeChart from '../components/RevenueOverTimeChart';
import TicketsSoldChart from '../components/TicketsSoldChart';
import RefundsOverTimeChart from '../components/RefundsOverTimeChart';
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

      {/* Stat Cards: single horizontal row, equal width */}
      <Box sx={{ display: 'flex', gap: 1.5, mb: 2, overflowX: 'auto' }}>
        {[
          { title: 'Total Events',          value: overview.totalEvents,                         icon: <EventIcon />,              color: '#e94560' },
          { title: 'Upcoming Events',        value: overview.upcomingEvents,                      icon: <UpcomingIcon />,            color: '#0f3460' },
          { title: 'Total Users',            value: overview.totalUsers,                          icon: <PeopleIcon />,              color: '#533483' },
          { title: 'Total Seats',            value: overview.totalSeats,                          icon: <ChairIcon />,               color: '#2196f3' },
          { title: 'Booked Seats',           value: overview.bookedSeats,                         icon: <ConfirmationNumberIcon />,  color: '#4caf50' },
          { title: 'Active Holds',           value: overview.activeHolds,                         icon: <PanToolIcon />,             color: '#ff9800' },
          { title: 'Sold Seats',             value: overview.soldSeats,                           icon: <ShoppingCartIcon />,        color: '#2196f3' },
          { title: 'Total Revenue',          value: `$${(data.totalRevenue || 0).toFixed(2)}`,    icon: <AttachMoneyIcon />,         color: '#4caf50' },
          { title: 'Confirmation Rate',      value: `${data.holdConfirmationRate}%`,              icon: <PercentIcon />,             color: '#4caf50' },
        ].map((card) => (
          <Box key={card.title} sx={{ flex: '1 1 0', minWidth: 140 }}>
            <StatsCard title={card.title} value={card.value} icon={card.icon} color={card.color} />
          </Box>
        ))}
      </Box>

      {/* Chart Row 1: all 6 status/activity charts side by side */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, overflowX: 'auto' }}>
        {[
          <SeatStatusChart data={data.seatsByStatus} />,
          <HoldStatusChart data={data.holdsByStatus} />,
          <HoldsOverTimeChart data={data.holdsPerDay} />,
          <TopEventsChart data={data.topEventsByBookings} />,
          <EventsByVenueChart data={data.eventsByVenue} />,
          <UsersByRoleChart data={data.usersByRole} />,
        ].map((chart, i) => (
          <Box key={i} sx={{ flex: '1 1 0', minWidth: 260 }}>
            {chart}
          </Box>
        ))}
      </Box>

      {/* Chart Row 2: Revenue + Tickets Sold + Refunds */}
      <Box sx={{ display: 'flex', gap: 2, overflowX: 'auto' }}>
        {[
          <RevenueOverTimeChart data={data.revenuePerDay} />,
          <TicketsSoldChart data={data.ticketsSoldPerDay} />,
          <RefundsOverTimeChart data={data.refundsPerDay} />,
        ].map((chart, i) => (
          <Box key={i} sx={{ flex: '1 1 0', minWidth: 260 }}>
            {chart}
          </Box>
        ))}
      </Box>
    </Box>
  );
}

export default AdminDashboard;
