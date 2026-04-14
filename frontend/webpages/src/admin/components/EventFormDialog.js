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

function EventFormDialog({ open, onClose, onSaved, event }) {
  const isEdit = Boolean(event);
  const [title, setTitle] = useState('');
  const [venue, setVenue] = useState('');
  const [startTime, setStartTime] = useState('');
  const [thumbnail, setThumbnail] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (event) {
      setTitle(event.title || '');
      setVenue(event.venue || '');
      setThumbnail(event.thumbnail || '');
      const dt = event.startTime ? new Date(event.startTime) : null;
      if (dt) {
        const local = new Date(dt.getTime() - dt.getTimezoneOffset() * 60000);
        setStartTime(local.toISOString().slice(0, 16));
      } else {
        setStartTime('');
      }
    } else {
      setTitle('');
      setVenue('');
      setStartTime('');
      setThumbnail('');
    }
    setError('');
  }, [event, open]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !startTime) {
      setError('Title and start time are required.');
      return;
    }
    if (!isEdit && !venue.trim()) {
      setError('Venue is required.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      const isoTime = new Date(startTime).toISOString();
      if (isEdit) {
        await api.put(`/api/events/${event.id}`, {
          title: title.trim(),
          startTime: isoTime,
          thumbnail: thumbnail.trim() || null,
        });
      } else {
        await api.post('/api/events', {
          title: title.trim(),
          venue: venue.trim(),
          startTime: isoTime,
          thumbnail: thumbnail.trim() || null,
        });
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to save event.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>{isEdit ? 'Edit Event' : 'Create Event'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Venue"
              value={venue}
              onChange={(e) => setVenue(e.target.value)}
              required={!isEdit}
              disabled={isEdit}
              fullWidth
              helperText={isEdit ? 'Venue cannot be changed after creation' : ''}
            />
            <TextField
              label="Start Time"
              type="datetime-local"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Thumbnail URL"
              value={thumbnail}
              onChange={(e) => setThumbnail(e.target.value)}
              placeholder="https://example.com/image.jpg"
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={saving}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving}>
            {saving ? 'Saving...' : isEdit ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

export default EventFormDialog;
