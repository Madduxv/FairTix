import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import Alert from '@mui/material/Alert';
import Breadcrumbs from '@mui/material/Breadcrumbs';
import Link from '@mui/material/Link';
import AddIcon from '@mui/icons-material/Add';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import MapIcon from '@mui/icons-material/Map';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';
import SeatFormDialog from '../components/SeatFormDialog';
import AdminSeatLayoutEditor from '../components/AdminSeatLayoutEditor';
import api from '../../api/client';

const statusColors = {
  AVAILABLE: 'success',
  HELD: 'warning',
  BOOKED: 'error',
  SOLD: 'primary',
};

function AdminSeatsPage() {
  const { eventId } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [csvDialogOpen, setCsvDialogOpen] = useState(false);
  const [csvFile, setCsvFile] = useState(null);
  const [csvUploading, setCsvUploading] = useState(false);
  const [csvError, setCsvError] = useState('');
  const [csvResult, setCsvResult] = useState(null);
  const [layoutEditorOpen, setLayoutEditorOpen] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [eventData, seatsData] = await Promise.all([
        api.get(`/api/events/${eventId}`),
        api.get(`/api/events/${eventId}/seats`),
      ]);
      setEvent(eventData);
      setSeats(seatsData || []);
    } catch (err) {
      setError(err.message || 'Failed to load seat data.');
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCsvUpload = async () => {
    if (!csvFile) return;
    setCsvUploading(true);
    setCsvError('');
    setCsvResult(null);
    try {
      const formData = new FormData();
      formData.append('file', csvFile);
      const result = await api.post(`/api/events/${eventId}/seats/import`, formData);
      setCsvResult(result);
      fetchData();
    } catch (err) {
      setCsvError(err.message || 'CSV import failed.');
    } finally {
      setCsvUploading(false);
    }
  };

  const statusSummary = seats.reduce((acc, seat) => {
    acc[seat.status] = (acc[seat.status] || 0) + 1;
    return acc;
  }, {});

  return (
    <Box>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link
          underline="hover"
          color="inherit"
          sx={{ cursor: 'pointer' }}
          onClick={() => navigate('/admin/events')}
        >
          Events
        </Link>
        <Typography color="text.primary">
          {event?.title || 'Loading...'}
        </Typography>
        <Typography color="text.primary">Seats</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            Seats {event ? `- ${event.title}` : ''}
          </Typography>
          {!loading && (
            <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
              <Chip label={`${seats.length} total`} size="small" />
              {Object.entries(statusSummary).map(([status, count]) => (
                <Chip
                  key={status}
                  label={`${count} ${status.toLowerCase()}`}
                  size="small"
                  color={statusColors[status] || 'default'}
                />
              ))}
            </Box>
          )}
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/admin/events')}
          >
            Back
          </Button>
          <Button variant="outlined" startIcon={<UploadFileIcon />} onClick={() => setCsvDialogOpen(true)}>
            Import CSV
          </Button>
          <Button variant="outlined" startIcon={<MapIcon />} onClick={() => setLayoutEditorOpen(true)}>
            Edit Layout
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setFormOpen(true)}>
            Add Seat
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Section</TableCell>
              <TableCell>Row</TableCell>
              <TableCell>Seat Number</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">Loading...</TableCell>
              </TableRow>
            ) : seats.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">No seats added yet.</TableCell>
              </TableRow>
            ) : (
              seats.map((seat) => (
                <TableRow key={seat.id} hover>
                  <TableCell>{seat.section}</TableCell>
                  <TableCell>{seat.rowLabel}</TableCell>
                  <TableCell>{seat.seatNumber}</TableCell>
                  <TableCell>${seat.price?.toFixed(2) ?? '0.00'}</TableCell>
                  <TableCell>
                    <Chip
                      label={seat.status}
                      size="small"
                      color={statusColors[seat.status] || 'default'}
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <SeatFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={fetchData}
        eventId={eventId}
      />

      {/* CSV Import Dialog */}
      <Dialog open={csvDialogOpen} onClose={() => { setCsvDialogOpen(false); setCsvFile(null); setCsvError(''); setCsvResult(null); }} maxWidth="sm" fullWidth>
        <DialogTitle>Import Seats from CSV</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            CSV format: <code>section,row,seat_number,price[,pos_x,pos_y,rotation]</code>
          </Typography>
          <TextField
            type="file"
            inputProps={{ accept: '.csv' }}
            onChange={(e) => setCsvFile(e.target.files[0])}
            fullWidth
            sx={{ mb: 2 }}
          />
          {csvError && <Alert severity="error" sx={{ mb: 1 }}>{csvError}</Alert>}
          {csvResult && (
            <Alert severity="success">
              Imported {csvResult.imported} seat(s).
              {csvResult.skipped > 0 && ` Skipped ${csvResult.skipped} duplicate(s).`}
              {csvResult.errors?.length > 0 && ` ${csvResult.errors.length} row error(s).`}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setCsvDialogOpen(false); setCsvFile(null); setCsvError(''); setCsvResult(null); }}>
            Close
          </Button>
          <Button
            variant="contained"
            onClick={handleCsvUpload}
            disabled={!csvFile || csvUploading}
            startIcon={csvUploading ? <CircularProgress size={16} /> : <UploadFileIcon />}
          >
            {csvUploading ? 'Uploading...' : 'Upload'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Layout Editor */}
      <AdminSeatLayoutEditor
        open={layoutEditorOpen}
        eventId={eventId}
        venueId={event?.venueId}
        onClose={() => { setLayoutEditorOpen(false); fetchData(); }}
      />
    </Box>
  );
}

export default AdminSeatsPage;
