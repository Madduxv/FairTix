import React, { useState, useEffect, useCallback } from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TablePagination from '@mui/material/TablePagination';
import Paper from '@mui/material/Paper';
import Chip from '@mui/material/Chip';
import Button from '@mui/material/Button';
import Alert from '@mui/material/Alert';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import DeleteIcon from '@mui/icons-material/Delete';
import ConfirmDialog from '../components/ConfirmDialog';
import api from '../../api/client';
import { useAuth } from '../../auth/useAuth';

function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [promoteOpen, setPromoteOpen] = useState(false);
  const [promotingUser, setPromotingUser] = useState(null);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deletingUser, setDeletingUser] = useState(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.get(`/api/admin/users?page=${page}&size=${rowsPerPage}`);
      setUsers(data.content || []);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(err.message || 'Failed to load users.');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handlePromoteClick = (user) => {
    setPromotingUser(user);
    setPromoteOpen(true);
  };

  const handlePromoteConfirm = async () => {
    try {
      await api.patch(`/api/admin/users/${promotingUser.id}/promote`);
      setSuccess(`${promotingUser.email} has been promoted to Admin.`);
      setPromoteOpen(false);
      setPromotingUser(null);
      fetchUsers();
    } catch (err) {
      setError(err.message || 'Failed to promote user.');
      setPromoteOpen(false);
    }
  };

  const handleDeleteClick = (user) => {
    setDeletingUser(user);
    setDeleteOpen(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await api.delete(`/api/admin/users/${deletingUser.id}`);
      setSuccess(`${deletingUser.email} has been deleted.`);
      setDeleteOpen(false);
      setDeletingUser(null);
      fetchUsers();
    } catch (err) {
      setError(err.message || 'Failed to delete user.');
      setDeleteOpen(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 3 }}>
        Users
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>{success}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={3} align="center">Loading...</TableCell>
              </TableRow>
            ) : users.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center">No users found.</TableCell>
              </TableRow>
            ) : (
              users.map((user) => (
                <TableRow key={user.id} hover>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>
                    <Chip
                      label={user.role}
                      size="small"
                      color={user.role === 'ADMIN' ? 'primary' : 'default'}
                    />
                  </TableCell>
                  <TableCell align="right" sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                    {user.role !== 'ADMIN' && (
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<AdminPanelSettingsIcon />}
                        onClick={() => handlePromoteClick(user)}
                      >
                        Promote to Admin
                      </Button>
                    )}
                    {user.id !== currentUser.userId && (
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        startIcon={<DeleteIcon />}
                        onClick={() => handleDeleteClick(user)}
                      >
                        Delete
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
          rowsPerPageOptions={[5, 10, 25]}
        />
      </TableContainer>

      <ConfirmDialog
        open={promoteOpen}
        onClose={() => setPromoteOpen(false)}
        onConfirm={handlePromoteConfirm}
        title="Promote User"
        message={`Are you sure you want to promote "${promotingUser?.email}" to Admin? This action cannot be undone.`}
      />

      <ConfirmDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        title="Delete User"
        message={`Are you sure you want to delete "${deletingUser?.email}"? This action is permanent and cannot be undone.`}
      />
    </Box>
  );
}

export default AdminUsersPage;
