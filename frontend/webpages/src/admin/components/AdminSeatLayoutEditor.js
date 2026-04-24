import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import Dialog from '@mui/material/Dialog';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Switch from '@mui/material/Switch';
import FormControlLabel from '@mui/material/FormControlLabel';
import Tooltip from '@mui/material/Tooltip';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import CloseIcon from '@mui/icons-material/Close';
import SaveIcon from '@mui/icons-material/Save';
import UndoIcon from '@mui/icons-material/Undo';
import RedoIcon from '@mui/icons-material/Redo';
import RestartAltIcon from '@mui/icons-material/RestartAlt';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import AddIcon from '@mui/icons-material/Add';
import api from '../../api/client';

const SEAT_SIZE = 28;
const SEAT_RADIUS = 5;
const SNAP_GRID = 10;
const CANVAS_PADDING = 60;

// Auto-layout constants (mirrors SeatMap.js)
const LABEL_WIDTH = 52;
const SECTION_LABEL_HEIGHT = 22;
const ROW_GAP = 10;
const SECTION_GAP = 36;
const PADDING_Y = 16;
const SEAT_GAP = 6;

function naturalCompare(a, b) {
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' });
}

function computeAutoLayout(seats) {
  const sectionMap = new Map();
  for (const seat of seats) {
    const sec = seat.section || 'General';
    if (!sectionMap.has(sec)) sectionMap.set(sec, new Map());
    const rowMap = sectionMap.get(sec);
    if (!rowMap.has(seat.rowLabel)) rowMap.set(seat.rowLabel, []);
    rowMap.get(seat.rowLabel).push(seat);
  }
  for (const rowMap of sectionMap.values()) {
    for (const rowSeats of rowMap.values()) {
      rowSeats.sort((a, b) => naturalCompare(a.seatNumber, b.seatNumber));
    }
  }

  const result = {};
  let currentY = PADDING_Y;
  for (const [, rowMap] of [...sectionMap.entries()].sort(([a], [b]) => a.localeCompare(b))) {
    currentY += SECTION_LABEL_HEIGHT;
    const sortedRows = [...rowMap.entries()].sort(([a], [b]) => a.localeCompare(b));
    for (const [, rowSeats] of sortedRows) {
      let cx = LABEL_WIDTH;
      for (const seat of rowSeats) {
        result[seat.id] = { posX: cx + SEAT_SIZE / 2, posY: currentY + SEAT_SIZE / 2 };
        cx += SEAT_SIZE + SEAT_GAP;
      }
      currentY += SEAT_SIZE + ROW_GAP;
    }
    currentY += SECTION_GAP - ROW_GAP;
  }
  return result;
}

function snap(val, enabled) {
  return enabled ? Math.round(val / SNAP_GRID) * SNAP_GRID : val;
}

export default function AdminSeatLayoutEditor({ open, onClose, eventId, venueId }) {
  const [seats, setSeats] = useState([]);
  const [positions, setPositions] = useState({});
  const [sections, setSections] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [snapEnabled, setSnapEnabled] = useState(true);
  const [canUndo, setCanUndo] = useState(false);
  const [canRedo, setCanRedo] = useState(false);

  const svgRef = useRef(null);
  const transformRef = useRef({ x: 0, y: 0, scale: 1 });
  const [transformState, setTransformState] = useState({ x: 0, y: 0, scale: 1 });
  const isPanning = useRef(false);
  const panStart = useRef(null);

  // Seat drag
  const dragging = useRef(null);

  // Section drag
  const draggingSection = useRef(null);

  // Positions ref — always current, used in event handlers to avoid stale closures
  const positionsRef = useRef({});
  const sectionsRef = useRef([]);

  // History stored in refs to avoid closure issues
  const historyRef = useRef([]);
  const historyIndexRef = useRef(-1);

  function syncHistoryUI() {
    setCanUndo(historyIndexRef.current > 0);
    setCanRedo(historyIndexRef.current < historyRef.current.length - 1);
  }

  function pushHistory(snapshot) {
    historyRef.current = historyRef.current.slice(0, historyIndexRef.current + 1);
    historyRef.current.push({ ...snapshot });
    historyIndexRef.current = historyRef.current.length - 1;
    syncHistoryUI();
  }

  // Load data when editor opens
  useEffect(() => {
    if (!open || !eventId) return;
    setLoading(true);
    setError('');
    setSuccess('');

    const sectionFetch = venueId
      ? api.get(`/api/venues/${venueId}/sections`)
      : Promise.resolve([]);

    Promise.all([api.get(`/api/events/${eventId}/seats/map`), sectionFetch])
      .then(([seatData, sectionData]) => {
        const seatList = seatData || [];
        const sectionList = sectionData || [];
        setSeats(seatList);
        setSections(sectionList);
        sectionsRef.current = sectionList;

        const initial = {};
        for (const s of seatList) {
          initial[s.id] = { posX: s.posX, posY: s.posY };
        }
        positionsRef.current = initial;
        setPositions({ ...initial });

        historyRef.current = [{ ...initial }];
        historyIndexRef.current = 0;
        syncHistoryUI();

        transformRef.current = { x: 0, y: 0, scale: 1 };
        setTransformState({ x: 0, y: 0, scale: 1 });
      })
      .catch((err) => setError(err.message || 'Failed to load layout data'))
      .finally(() => setLoading(false));
  }, [open, eventId, venueId]);

  const undo = useCallback(() => {
    if (historyIndexRef.current <= 0) return;
    historyIndexRef.current--;
    const snapshot = historyRef.current[historyIndexRef.current];
    positionsRef.current = { ...snapshot };
    setPositions({ ...snapshot });
    syncHistoryUI();
  }, []);

  const redo = useCallback(() => {
    if (historyIndexRef.current >= historyRef.current.length - 1) return;
    historyIndexRef.current++;
    const snapshot = historyRef.current[historyIndexRef.current];
    positionsRef.current = { ...snapshot };
    setPositions({ ...snapshot });
    syncHistoryUI();
  }, []);

  const autoLayout = useCallback(() => {
    const computed = computeAutoLayout(seats);
    const next = { ...positionsRef.current, ...computed };
    positionsRef.current = next;
    setPositions({ ...next });
    pushHistory(next);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seats]);

  const resetToSaved = useCallback(() => {
    const saved = historyRef.current[0];
    positionsRef.current = { ...saved };
    setPositions({ ...saved });
    historyRef.current = [{ ...saved }];
    historyIndexRef.current = 0;
    syncHistoryUI();
  }, []);

  // Convert screen coords to SVG content coords (accounting for <g> transform)
  const toSvgCoords = useCallback((clientX, clientY) => {
    const rect = svgRef.current?.getBoundingClientRect();
    if (!rect) return { x: 0, y: 0 };
    const t = transformRef.current;
    return {
      x: (clientX - rect.left - t.x) / t.scale,
      y: (clientY - rect.top - t.y) / t.scale,
    };
  }, []);

  const handleSeatMouseDown = useCallback((e, seatId) => {
    e.stopPropagation();
    e.preventDefault();
    const pt = toSvgCoords(e.clientX, e.clientY);
    const pos = positionsRef.current[seatId] || { posX: 0, posY: 0 };
    dragging.current = {
      seatId,
      startX: pt.x,
      startY: pt.y,
      origPosX: pos.posX,
      origPosY: pos.posY,
    };
  }, [toSvgCoords]);

  const handleSectionMouseDown = useCallback((e, section) => {
    e.stopPropagation();
    e.preventDefault();
    const pt = toSvgCoords(e.clientX, e.clientY);
    draggingSection.current = {
      sectionId: section.id,
      startX: pt.x,
      startY: pt.y,
      origPosX: section.posX,
      origPosY: section.posY,
    };
  }, [toSvgCoords]);

  const handleMouseDown = useCallback((e) => {
    if (dragging.current || draggingSection.current) return;
    if (e.button !== 0) return;
    isPanning.current = true;
    panStart.current = {
      x: e.clientX - transformRef.current.x,
      y: e.clientY - transformRef.current.y,
    };
  }, []);

  const handleMouseMove = useCallback((e) => {
    if (dragging.current) {
      const pt = toSvgCoords(e.clientX, e.clientY);
      const dx = pt.x - dragging.current.startX;
      const dy = pt.y - dragging.current.startY;
      const newX = snap(dragging.current.origPosX + dx, snapEnabled);
      const newY = snap(dragging.current.origPosY + dy, snapEnabled);
      const next = { ...positionsRef.current, [dragging.current.seatId]: { posX: newX, posY: newY } };
      positionsRef.current = next;
      setPositions({ ...next });
    } else if (draggingSection.current) {
      const pt = toSvgCoords(e.clientX, e.clientY);
      const dx = pt.x - draggingSection.current.startX;
      const dy = pt.y - draggingSection.current.startY;
      const newX = snap(draggingSection.current.origPosX + dx, snapEnabled);
      const newY = snap(draggingSection.current.origPosY + dy, snapEnabled);
      const updated = sectionsRef.current.map((s) =>
        s.id === draggingSection.current.sectionId ? { ...s, posX: newX, posY: newY } : s
      );
      sectionsRef.current = updated;
      setSections([...updated]);
    } else if (isPanning.current && panStart.current) {
      const newX = e.clientX - panStart.current.x;
      const newY = e.clientY - panStart.current.y;
      transformRef.current = { ...transformRef.current, x: newX, y: newY };
      setTransformState((prev) => ({ ...prev, x: newX, y: newY }));
    }
  }, [toSvgCoords, snapEnabled]);

  const handleMouseUp = useCallback((e) => {
    if (dragging.current) {
      pushHistory({ ...positionsRef.current });
      dragging.current = null;
    }
    if (draggingSection.current) {
      // Persist updated section position to server
      const sec = sectionsRef.current.find((s) => s.id === draggingSection.current.sectionId);
      if (sec && venueId) {
        api.put(`/api/venues/${venueId}/sections/${sec.id}`, {
          name: sec.name,
          sectionType: sec.sectionType,
          posX: sec.posX,
          posY: sec.posY,
          width: sec.width,
          height: sec.height,
          color: sec.color,
          sortOrder: sec.sortOrder,
        }).catch(() => {});
      }
      draggingSection.current = null;
    }
    if (isPanning.current) {
      isPanning.current = false;
      if (e.currentTarget) e.currentTarget.style.cursor = 'grab';
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [venueId]);

  const handleWheel = useCallback((e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    const newScale = Math.min(Math.max(transformRef.current.scale * delta, 0.2), 5);
    transformRef.current = { ...transformRef.current, scale: newScale };
    setTransformState((prev) => ({ ...prev, scale: newScale }));
  }, []);

  const handleAddSection = useCallback(async () => {
    if (!venueId) return;
    const name = window.prompt('Section name:');
    if (!name || !name.trim()) return;
    try {
      const created = await api.post(`/api/venues/${venueId}/sections`, {
        name: name.trim(),
        sectionType: 'STANDARD',
        posX: 50,
        posY: 50,
        width: 200,
        height: 120,
        color: '#90CAF9',
        sortOrder: sectionsRef.current.length,
      });
      const next = [...sectionsRef.current, created];
      sectionsRef.current = next;
      setSections(next);
    } catch (err) {
      setError(err.message || 'Failed to add section');
    }
  }, [venueId]);

  const handleDeleteSection = useCallback(async (sectionId) => {
    if (!venueId) return;
    try {
      await api.delete(`/api/venues/${venueId}/sections/${sectionId}`);
      const next = sectionsRef.current.filter((s) => s.id !== sectionId);
      sectionsRef.current = next;
      setSections(next);
    } catch (err) {
      setError(err.message || 'Failed to delete section');
    }
  }, [venueId]);

  const handleSave = useCallback(async () => {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const updates = Object.entries(positionsRef.current).map(([id, pos]) => ({
        id,
        posX: pos.posX,
        posY: pos.posY,
        rotation: null,
      }));
      await api.put(`/api/events/${eventId}/seats/positions`, updates);
      setSuccess(`Saved ${updates.length} seat position(s).`);
      // Reset undo baseline to the current state
      historyRef.current = [{ ...positionsRef.current }];
      historyIndexRef.current = 0;
      syncHistoryUI();
    } catch (err) {
      setError(err.message || 'Failed to save positions');
    } finally {
      setSaving(false);
    }
  }, [eventId]);

  // Compute canvas bounds from current positions + sections
  const { canvasWidth, canvasHeight } = useMemo(() => {
    let maxX = 400;
    let maxY = 300;
    for (const pos of Object.values(positions)) {
      const rx = pos.posX + SEAT_SIZE / 2 + CANVAS_PADDING;
      const ry = pos.posY + SEAT_SIZE / 2 + CANVAS_PADDING;
      if (rx > maxX) maxX = rx;
      if (ry > maxY) maxY = ry;
    }
    for (const sec of sections) {
      const rx = sec.posX + sec.width + CANVAS_PADDING;
      const ry = sec.posY + sec.height + CANVAS_PADDING;
      if (rx > maxX) maxX = rx;
      if (ry > maxY) maxY = ry;
    }
    return { canvasWidth: maxX, canvasHeight: maxY };
  }, [positions, sections]);

  const { x: tx, y: ty, scale: ts } = transformState;

  return (
    <Dialog fullScreen open={open} onClose={onClose}>
      <AppBar sx={{ position: 'relative', bgcolor: '#1a237e' }}>
        <Toolbar sx={{ gap: 1 }}>
          <IconButton edge="start" color="inherit" onClick={onClose} aria-label="close editor">
            <CloseIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flex: 1 }}>
            Edit Seat Layout
          </Typography>

          <FormControlLabel
            control={
              <Switch
                size="small"
                checked={snapEnabled}
                onChange={(e) => setSnapEnabled(e.target.checked)}
                color="default"
              />
            }
            label={<Typography variant="caption" sx={{ color: 'white' }}>Snap {SNAP_GRID}px</Typography>}
          />

          <Tooltip title="Undo (last drag)">
            <span>
              <IconButton color="inherit" onClick={undo} disabled={!canUndo} size="small">
                <UndoIcon />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Redo">
            <span>
              <IconButton color="inherit" onClick={redo} disabled={!canRedo} size="small">
                <RedoIcon />
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Auto-layout — reset all seats to computed grid positions">
            <IconButton color="inherit" onClick={autoLayout} size="small">
              <AutoFixHighIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Reset to last saved state">
            <IconButton color="inherit" onClick={resetToSaved} size="small">
              <RestartAltIcon />
            </IconButton>
          </Tooltip>
          {venueId && (
            <Tooltip title="Add section overlay">
              <IconButton color="inherit" onClick={handleAddSection} size="small">
                <AddIcon />
              </IconButton>
            </Tooltip>
          )}

          <Button
            variant="contained"
            color="success"
            size="small"
            startIcon={saving ? <CircularProgress size={14} color="inherit" /> : <SaveIcon />}
            onClick={handleSave}
            disabled={saving || loading}
            sx={{ ml: 1 }}
          >
            Save
          </Button>
        </Toolbar>
      </AppBar>

      <Box sx={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 64px)', bgcolor: '#f0f4f8' }}>
        {error && (
          <Alert severity="error" onClose={() => setError('')} sx={{ mx: 2, mt: 1 }}>
            {error}
          </Alert>
        )}
        {success && (
          <Alert severity="success" onClose={() => setSuccess('')} sx={{ mx: 2, mt: 1 }}>
            {success}
          </Alert>
        )}

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Box
            sx={{ flex: 1, overflow: 'hidden', cursor: 'grab', position: 'relative' }}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
            onWheel={handleWheel}
          >
            <svg
              ref={svgRef}
              style={{ width: '100%', height: '100%', display: 'block', userSelect: 'none' }}
            >
              <g transform={`translate(${tx} ${ty}) scale(${ts})`}>
                {/* Grid dots (subtle background guide) */}
                {snapEnabled && (
                  <pattern id="grid" width={SNAP_GRID} height={SNAP_GRID} patternUnits="userSpaceOnUse">
                    <circle cx={SNAP_GRID / 2} cy={SNAP_GRID / 2} r="0.8" fill="#ccc" />
                  </pattern>
                )}
                {snapEnabled && (
                  <rect width={canvasWidth} height={canvasHeight} fill="url(#grid)" />
                )}

                {/* Section overlays */}
                {sections.map((sec) => (
                  <g key={sec.id}>
                    <rect
                      x={sec.posX}
                      y={sec.posY}
                      width={sec.width}
                      height={sec.height}
                      fill={sec.color || '#E0E0E0'}
                      fillOpacity={0.2}
                      stroke={sec.color || '#999'}
                      strokeWidth={1.5}
                      strokeDasharray="6 3"
                      rx={4}
                      style={{ cursor: 'move' }}
                      onMouseDown={(e) => handleSectionMouseDown(e, sec)}
                    />
                    <text
                      x={sec.posX + 8}
                      y={sec.posY + 15}
                      fontSize="10"
                      fill={sec.color || '#555'}
                      fontWeight="700"
                      letterSpacing="0.05em"
                      style={{ pointerEvents: 'none', userSelect: 'none' }}
                    >
                      {sec.name.toUpperCase()}
                    </text>
                    {/* Delete button (×) in top-right corner */}
                    <text
                      x={sec.posX + sec.width - 10}
                      y={sec.posY + 13}
                      fontSize="12"
                      fill="#c62828"
                      fontWeight="bold"
                      style={{ cursor: 'pointer', userSelect: 'none' }}
                      onClick={(e) => { e.stopPropagation(); handleDeleteSection(sec.id); }}
                    >
                      ×
                    </text>
                  </g>
                ))}

                {/* Seats */}
                {seats.map((seat) => {
                  const pos = positions[seat.id] || { posX: seat.posX, posY: seat.posY };
                  return (
                    <g key={seat.id}>
                      <rect
                        x={pos.posX - SEAT_SIZE / 2}
                        y={pos.posY - SEAT_SIZE / 2}
                        width={SEAT_SIZE}
                        height={SEAT_SIZE}
                        rx={SEAT_RADIUS}
                        ry={SEAT_RADIUS}
                        fill="#e8f5e9"
                        stroke="#2e7d32"
                        strokeWidth={1.5}
                        style={{ cursor: 'move' }}
                        onMouseDown={(e) => handleSeatMouseDown(e, seat.id)}
                      />
                      <text
                        x={pos.posX}
                        y={pos.posY + 4}
                        textAnchor="middle"
                        fontSize="8"
                        fill="#1b5e20"
                        style={{ pointerEvents: 'none', userSelect: 'none' }}
                      >
                        {seat.seatNumber}
                      </text>
                    </g>
                  );
                })}
              </g>
            </svg>
          </Box>
        )}

        <Box
          sx={{
            px: 2,
            py: 0.75,
            bgcolor: 'white',
            borderTop: '1px solid #e0e0e0',
            display: 'flex',
            alignItems: 'center',
            gap: 3,
          }}
        >
          <Typography variant="caption" color="text.secondary">
            Drag seats to reposition &bull; Drag section boxes to move overlays &bull; Scroll to zoom &bull; Click+drag background to pan
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
            {seats.length} seats &bull; {sections.length} sections &bull; scale {ts.toFixed(1)}×
          </Typography>
        </Box>
      </Box>
    </Dialog>
  );
}
