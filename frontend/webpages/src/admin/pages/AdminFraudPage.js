import React, { useState, useEffect, useCallback } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TablePagination from '@mui/material/TablePagination';
import Paper from '@mui/material/Paper';
import Alert from '@mui/material/Alert';
import Chip from '@mui/material/Chip';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Divider from '@mui/material/Divider';
import CircularProgress from '@mui/material/CircularProgress';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../../api/client';

const SEVERITY_COLORS = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'warning',
  CRITICAL: 'error',
};

const SEVERITY_SX = {
  HIGH: { backgroundColor: 'orange', color: '#fff' },
};

const TIER_COLORS = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'warning',
  CRITICAL: 'error',
};

const TIER_SX = {
  HIGH: { backgroundColor: 'orange', color: '#fff' },
};

function AdminFraudPage() {
  const [flags, setFlags] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [filterUserId, setFilterUserId] = useState('');
  const [filterSeverity, setFilterSeverity] = useState('ALL');
  const [appliedUserId, setAppliedUserId] = useState('');
  const [appliedSeverity, setAppliedSeverity] = useState('ALL');

  const [resolveOpen, setResolveOpen] = useState(false);
  const [resolvingFlag, setResolvingFlag] = useState(null);
  const [resolveError, setResolveError] = useState('');

  const [riskUserId, setRiskUserId] = useState('');
  const [riskResult, setRiskResult] = useState(null);
  const [riskLoading, setRiskLoading] = useState(false);
  const [riskError, setRiskError] = useState('');

  const fetchFlags = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      let url = `/api/admin/fraud/flags?page=${page}&size=${rowsPerPage}`;
      if (appliedUserId) url += `&userId=${encodeURIComponent(appliedUserId)}`;
      if (appliedSeverity !== 'ALL') url += `&severity=${appliedSeverity}`;
      const data = await api.get(url);
      setFlags(data.content || []);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message || 'Failed to load fraud flags.');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, appliedUserId, appliedSeverity]);

  useEffect(() => {
    fetchFlags();
  }, [fetchFlags]);

  const handleSearch = () => {
    setPage(0);
    setAppliedUserId(filterUserId.trim());
    setAppliedSeverity(filterSeverity);
  };

  const handleResolveClick = (flag) => {
    setResolvingFlag(flag);
    setResolveError('');
    setResolveOpen(true);
  };

  const handleResolveConfirm = async () => {
    try {
      await api.patch(`/api/admin/fraud/flags/${resolvingFlag.id}/resolve`);
      setResolveOpen(false);
      setResolvingFlag(null);
      fetchFlags();
    } catch (err) {
      setResolveError(err.message || 'Failed to resolve flag.');
      setResolveOpen(false);
    }
  };

  const handleRiskLookup = async () => {
    if (!riskUserId.trim()) return;
    setRiskLoading(true);
    setRiskError('');
    setRiskResult(null);
    try {
      const data = await api.get(`/api/admin/fraud/risk/${encodeURIComponent(riskUserId.trim())}`);
      setRiskResult(data);
    } catch (err) {
      if (err.status === 404 || (err.message && err.message.includes('404'))) {
        setRiskError('No risk score record found for this user.');
      } else {
        setRiskError(err.message || 'Failed to fetch risk score.');
      }
    } finally {
      setRiskLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
        Fraud Dashboard
      </Typography>

      {/* Section 1: Suspicious Flags */}
      <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
        Suspicious Flags
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap', alignItems: 'center' }}>
        <TextField
          label="User ID"
          value={filterUserId}
          onChange={(e) => setFilterUserId(e.target.value)}
          size="small"
          sx={{ minWidth: 280 }}
          placeholder="Enter exact user UUID"
        />
        <TextField
          select
          label="Severity"
          value={filterSeverity}
          onChange={(e) => setFilterSeverity(e.target.value)}
          size="small"
          sx={{ minWidth: 140 }}
        >
          {['ALL', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((s) => (
            <MenuItem key={s} value={s}>{s}</MenuItem>
          ))}
        </TextField>
        <Button variant="contained" onClick={handleSearch}>
          Search
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {resolveError && <Alert severity="error" sx={{ mb: 2 }}>{resolveError}</Alert>}

      <TableContainer component={Paper} sx={{ mb: 4 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>User ID</TableCell>
              <TableCell>Flag Type</TableCell>
              <TableCell>Severity</TableCell>
              <TableCell>Details</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center">Loading...</TableCell>
              </TableRow>
            ) : flags.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">No flags found.</TableCell>
              </TableRow>
            ) : (
              flags.map((flag) => (
                <TableRow key={flag.id} hover>
                  <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                    {flag.userId}
                  </TableCell>
                  <TableCell>{flag.flagType}</TableCell>
                  <TableCell>
                    <Chip
                      label={flag.severity}
                      size="small"
                      color={SEVERITY_COLORS[flag.severity] || 'default'}
                      sx={SEVERITY_SX[flag.severity] || {}}
                    />
                  </TableCell>
                  <TableCell sx={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {flag.details || '—'}
                  </TableCell>
                  <TableCell>{flag.createdAt ? new Date(flag.createdAt).toLocaleString() : '—'}</TableCell>
                  <TableCell>
                    <Chip
                      label={flag.status}
                      size="small"
                      color={flag.status === 'RESOLVED' ? 'success' : 'warning'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    {flag.status === 'OPEN' && (
                      <Button
                        size="small"
                        variant="outlined"
                        color="warning"
                        onClick={() => handleResolveClick(flag)}
                      >
                        Resolve
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRowsPerPage(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </TableContainer>

      <Divider sx={{ my: 4 }} />

      {/* Section 2: Risk Score Lookup */}
      <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
        Risk Score Lookup
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <TextField
          label="User ID"
          value={riskUserId}
          onChange={(e) => setRiskUserId(e.target.value)}
          size="small"
          sx={{ minWidth: 280 }}
          placeholder="Enter exact user UUID"
          onKeyDown={(e) => { if (e.key === 'Enter') handleRiskLookup(); }}
        />
        <Button variant="contained" onClick={handleRiskLookup} disabled={riskLoading}>
          {riskLoading ? <CircularProgress size={20} /> : 'Lookup'}
        </Button>
      </Box>

      {riskError && <Alert severity="error" sx={{ mb: 2 }}>{riskError}</Alert>}

      {riskResult && (
        <Paper sx={{ p: 3, maxWidth: 480 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
            <Typography variant="h3" sx={{ fontWeight: 700 }}>
              {riskResult.score}
            </Typography>
            <Typography variant="body2" color="text.secondary">/ 100</Typography>
            <Chip
              label={riskResult.tier}
              color={TIER_COLORS[riskResult.tier] || 'default'}
              sx={TIER_SX[riskResult.tier] || {}}
            />
          </Box>
          <Typography variant="body2" color="text.secondary">
            Flag count: <strong>{riskResult.flagCount ?? '—'}</strong>
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Last calculated:{' '}
            <strong>
              {riskResult.lastCalculated ? new Date(riskResult.lastCalculated).toLocaleString() : '—'}
            </strong>
          </Typography>
        </Paper>
      )}

      <ConfirmDialog
        open={resolveOpen}
        onClose={() => setResolveOpen(false)}
        onConfirm={handleResolveConfirm}
        title="Resolve Flag"
        message={`Are you sure you want to resolve this flag? This action cannot be undone.`}
      />
    </Box>
  );
}

export default AdminFraudPage;
