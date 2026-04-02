import React, { useState, useEffect } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Alert from '@mui/material/Alert';
import api from '../../api/client';

function SeatFormDialog({ open, onClose, onSaved, eventId }) {
  const [section, setSection] = useState('');
  const [rowLabel, setRowLabel] = useState('');
  const [seatNumber, setSeatNumber] = useState('');
  const [price, setPrice] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setSection('');
      setRowLabel('');
      setSeatNumber('');
      setPrice('');
      setError('');
    }
  }, [open]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!section.trim() || !rowLabel.trim() || !seatNumber.trim() || price === '') {
      setError('All fields are required.');
      return;
    }
    if (isNaN(parseFloat(price)) || parseFloat(price) < 0) {
      setError('Price must be a non-negative number.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      await api.post(`/api/events/${eventId}/seats`, {
        section: section.trim(),
        rowLabel: rowLabel.trim(),
        seatNumber: seatNumber.trim(),
        price: parseFloat(price),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to create seat.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Add Seat</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Section"
              value={section}
              onChange={(e) => setSection(e.target.value)}
              required
              fullWidth
              placeholder="e.g. Floor, Balcony"
            />
            <TextField
              label="Row Label"
              value={rowLabel}
              onChange={(e) => setRowLabel(e.target.value)}
              required
              fullWidth
              placeholder="e.g. A, B, C"
            />
            <TextField
              label="Seat Number"
              value={seatNumber}
              onChange={(e) => setSeatNumber(e.target.value)}
              required
              fullWidth
              placeholder="e.g. 101, 102"
            />
            <TextField
              label="Price"
              type="number"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
              fullWidth
              placeholder="e.g. 49.99"
              inputProps={{ min: 0, step: '0.01' }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving}>
            {saving ? 'Adding...' : 'Add Seat'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

export default SeatFormDialog;
