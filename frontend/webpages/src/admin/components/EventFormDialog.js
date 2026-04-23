import React, { useState, useEffect } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import TextField from '@mui/material/TextField';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Alert from '@mui/material/Alert';
import MenuItem from '@mui/material/MenuItem';
import OutlinedInput from '@mui/material/OutlinedInput';
import InputLabel from '@mui/material/InputLabel';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';
import Chip from '@mui/material/Chip';
import api from '../../api/client';

function EventFormDialog({ open, onClose, onSaved, event }) {
  const isEdit = Boolean(event);
  const [title, setTitle] = useState('');
  const [venueId, setVenueId] = useState('');
  const [startTime, setStartTime] = useState('');
  const [queueRequired, setQueueRequired] = useState(false);
  const [queueCapacity, setQueueCapacity] = useState('');
  const [maxTicketsPerUser, setMaxTicketsPerUser] = useState('');
  const [venues, setVenues] = useState([]);
  const [performerOptions, setPerformerOptions] = useState([]);
  const [selectedPerformerIds, setSelectedPerformerIds] = useState([]);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    api.get('/api/venues?size=100').then((data) => {
      setVenues(data.content || []);
    }).catch(() => {});
    api.get('/api/performers?page=0&size=100').then((data) => {
      setPerformerOptions(data.content || []);
    }).catch(() => {});
  }, [open]);

  useEffect(() => {
    if (event) {
      setTitle(event.title || '');
      setVenueId(event.venue?.id || '');
      const dt = event.startTime ? new Date(event.startTime) : null;
      if (dt) {
        const local = new Date(dt.getTime() - dt.getTimezoneOffset() * 60000);
        setStartTime(local.toISOString().slice(0, 16));
      } else {
        setStartTime('');
      }
      setQueueRequired(event.queueRequired || false);
      setQueueCapacity(event.queueCapacity != null ? String(event.queueCapacity) : '');
      setMaxTicketsPerUser(event.maxTicketsPerUser != null ? String(event.maxTicketsPerUser) : '');
      setSelectedPerformerIds((event.performers || []).map((p) => p.id));
    } else {
      setTitle('');
      setVenueId('');
      setStartTime('');
      setQueueRequired(false);
      setQueueCapacity('');
      setMaxTicketsPerUser('');
      setSelectedPerformerIds([]);
    }
    setError('');
  }, [event, open]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !startTime) {
      setError('Title and start time are required.');
      return;
    }
    if (!isEdit && !venueId) {
      setError('Venue is required.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      const isoTime = new Date(startTime).toISOString();
      const capacity = queueCapacity !== '' ? parseInt(queueCapacity, 10) : null;
      const cap = maxTicketsPerUser !== '' ? parseInt(maxTicketsPerUser, 10) : null;
      if (isEdit) {
        await api.put(`/api/events/${event.id}`, {
          title: title.trim(),
          startTime: isoTime,
          queueRequired,
          queueCapacity: capacity,
          maxTicketsPerUser: cap,
          performerIds: selectedPerformerIds,
        });
      } else {
        await api.post('/api/events', {
          title: title.trim(),
          venueId,
          startTime: isoTime,
          queueRequired,
          queueCapacity: capacity,
          maxTicketsPerUser: cap,
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
              select
              label="Venue"
              value={venueId}
              onChange={(e) => setVenueId(e.target.value)}
              required={!isEdit}
              disabled={isEdit}
              fullWidth
              helperText={isEdit ? 'Venue cannot be changed after creation' : ''}
            >
              {venues.map((v) => (
                <MenuItem key={v.id} value={v.id}>{v.name}</MenuItem>
              ))}
            </TextField>
            <TextField
              label="Start Time"
              type="datetime-local"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={queueRequired}
                  onChange={(e) => setQueueRequired(e.target.checked)}
                />
              }
              label="Require Queue Admission"
            />
            {queueRequired && (
              <TextField
                label="Queue Capacity (leave blank for unlimited)"
                type="number"
                value={queueCapacity}
                onChange={(e) => setQueueCapacity(e.target.value)}
                fullWidth
                inputProps={{ min: 1 }}
              />
            )}
            <TextField
              label="Max Tickets Per User (leave blank for no limit)"
              type="number"
              value={maxTicketsPerUser}
              onChange={(e) => setMaxTicketsPerUser(e.target.value)}
              fullWidth
              inputProps={{ min: 1 }}
            />
            <FormControl fullWidth>
              <InputLabel>Performers</InputLabel>
              <Select
                multiple
                value={selectedPerformerIds}
                onChange={(e) => setSelectedPerformerIds(e.target.value)}
                input={<OutlinedInput label="Performers" />}
                renderValue={(selected) => (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {selected.map((id) => {
                      const p = performerOptions.find((o) => o.id === id);
                      return <Chip key={id} label={p ? p.name : id} size="small" />;
                    })}
                  </Box>
                )}
              >
                {performerOptions.map((p) => (
                  <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
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
