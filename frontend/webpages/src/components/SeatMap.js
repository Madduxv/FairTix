import { useState, useRef, useCallback, useMemo } from 'react';

const SEAT_SIZE = 28;
const SEAT_RADIUS = 5;
const LABEL_WIDTH = 52;
const SECTION_LABEL_HEIGHT = 22;
const ROW_GAP = 10;
const SECTION_GAP = 36;
const PADDING_Y = 16;
const SEAT_GAP = 6;

const STATUS_COLORS = {
  AVAILABLE: { fill: '#e6f4ea', stroke: '#1e7e34', text: '#1e7e34' },
  HELD:      { fill: '#fff8e1', stroke: '#f9a825', text: '#f9a825' },
  BOOKED:    { fill: '#fce8e6', stroke: '#c5221f', text: '#c5221f' },
  SOLD:      { fill: '#e8f0fe', stroke: '#1a73e8', text: '#1a73e8' },
  SELECTED:  { fill: '#1e7e34', stroke: '#145a26', text: '#fff' },
};

/**
 * Builds layout data from a flat seat list grouped by section → row.
 * Returns { sections, seats: [{...seat, cx, cy}], totalWidth, totalHeight }
 */
function buildLayout(seats) {
  // Group: section → row → seats
  const sectionMap = new Map();
  for (const seat of seats) {
    const sec = seat.section || 'General';
    if (!sectionMap.has(sec)) sectionMap.set(sec, new Map());
    const rowMap = sectionMap.get(sec);
    if (!rowMap.has(seat.rowLabel)) rowMap.set(seat.rowLabel, []);
    rowMap.get(seat.rowLabel).push(seat);
  }

  // Sort rows and seats within each section
  for (const rowMap of sectionMap.values()) {
    for (const rowSeats of rowMap.values()) {
      rowSeats.sort((a, b) => naturalCompare(a.seatNumber, b.seatNumber));
    }
  }

  const sections = [];
  const layoutSeats = [];
  let maxRowWidth = 0;
  let currentY = PADDING_Y;

  for (const [sectionName, rowMap] of [...sectionMap.entries()].sort(([a], [b]) => a.localeCompare(b))) {
    const sectionStartY = currentY;
    currentY += SECTION_LABEL_HEIGHT;

    const sortedRows = [...rowMap.entries()].sort(([a], [b]) => a.localeCompare(b));
    for (const [rowLabel, rowSeats] of sortedRows) {
      const rowStartX = LABEL_WIDTH;
      let cx = rowStartX;
      for (const seat of rowSeats) {
        layoutSeats.push({ ...seat, cx: cx + SEAT_SIZE / 2, cy: currentY + SEAT_SIZE / 2 });
        cx += SEAT_SIZE + SEAT_GAP;
      }
      const rowWidth = rowStartX + rowSeats.length * (SEAT_SIZE + SEAT_GAP);
      if (rowWidth > maxRowWidth) maxRowWidth = rowWidth;
      currentY += SEAT_SIZE + ROW_GAP;
    }

    sections.push({ name: sectionName, y: sectionStartY, rowCount: sortedRows.length });
    currentY += SECTION_GAP - ROW_GAP;
  }

  const totalHeight = currentY + PADDING_Y;
  const totalWidth = Math.max(maxRowWidth + 16, 300);

  return { sections, seats: layoutSeats, totalWidth, totalHeight };
}

function naturalCompare(a, b) {
  return a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' });
}

function SeatMap({ seats, selectedSeatIds, onToggleSeat, canSelect }) {
  const [tooltip, setTooltip] = useState(null);
  const [transform, setTransform] = useState({ x: 0, y: 0, scale: 1 });
  const isPanning = useRef(false);
  const panStart = useRef(null);
  const svgRef = useRef(null);

  const { sections, seats: layoutSeats, totalWidth, totalHeight } = useMemo(() => buildLayout(seats), [seats]);

  const handleMouseDown = useCallback((e) => {
    if (e.button !== 0) return;
    isPanning.current = true;
    panStart.current = { x: e.clientX - transform.x, y: e.clientY - transform.y };
    e.currentTarget.style.cursor = 'grabbing';
  }, [transform]);

  const handleMouseMove = useCallback((e) => {
    if (!isPanning.current) return;
    setTransform((prev) => ({
      ...prev,
      x: e.clientX - panStart.current.x,
      y: e.clientY - panStart.current.y,
    }));
  }, []);

  const handleMouseUp = useCallback((e) => {
    isPanning.current = false;
    if (e.currentTarget) e.currentTarget.style.cursor = 'grab';
  }, []);

  const handleWheel = useCallback((e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? 0.9 : 1.1;
    setTransform((prev) => {
      const newScale = Math.min(Math.max(prev.scale * delta, 0.4), 4);
      return { ...prev, scale: newScale };
    });
  }, []);

  function handleSeatClick(e, seat) {
    e.stopPropagation();
    if (canSelect && seat.status === 'AVAILABLE') {
      onToggleSeat(seat.id);
    }
  }

  function handleSeatMouseEnter(e, seat) {
    const rect = svgRef.current?.getBoundingClientRect();
    if (!rect) return;
    setTooltip({
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
      seat,
    });
  }

  function handleSeatMouseLeave() {
    setTooltip(null);
  }

  function resetView() {
    setTransform({ x: 0, y: 0, scale: 1 });
  }

  return (
    <div className="seat-map-wrapper">
      <div className="seat-map-controls">
        <button className="seat-map-zoom-btn" onClick={() => setTransform((p) => ({ ...p, scale: Math.min(p.scale * 1.2, 4) }))}>+</button>
        <button className="seat-map-zoom-btn" onClick={() => setTransform((p) => ({ ...p, scale: Math.max(p.scale * 0.8, 0.4) }))}>−</button>
        <button className="seat-map-zoom-btn seat-map-reset-btn" onClick={resetView}>Reset</button>
      </div>

      <div
        className="seat-map-container"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onWheel={handleWheel}
        style={{ cursor: 'grab' }}
      >
        <svg
          ref={svgRef}
          width="100%"
          height="100%"
          viewBox={`0 0 ${totalWidth} ${totalHeight}`}
          role="img"
          aria-label={`Seat map with ${layoutSeats.length} seats across ${sections.length} sections`}
          style={{ transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.scale})`, transformOrigin: 'center center', transition: isPanning.current ? 'none' : 'transform 0.1s ease' }}
        >
          {/* Section labels */}
          {sections.map((sec) => (
            <text
              key={sec.name}
              x={LABEL_WIDTH}
              y={sec.y + 14}
              className="seat-map-section-label"
              fontSize="11"
              fontWeight="700"
              fill="#555"
              letterSpacing="0.04em"
              textAnchor="start"
            >
              {sec.name.toUpperCase()}
            </text>
          ))}

          {/* Row labels */}
          {layoutSeats
            .filter((s, i, arr) => arr.findIndex((o) => o.rowLabel === s.rowLabel && o.section === s.section) === i)
            .map((s) => (
              <text
                key={`${s.section}-${s.rowLabel}`}
                x={LABEL_WIDTH - 6}
                y={s.cy + 5}
                fontSize="10"
                fill="#888"
                textAnchor="end"
              >
                {s.rowLabel}
              </text>
            ))}

          {/* Seats */}
          {layoutSeats.map((seat) => {
            const isSelected = selectedSeatIds?.has(seat.id);
            const key = isSelected ? 'SELECTED' : seat.status;
            const colors = STATUS_COLORS[key] || STATUS_COLORS.AVAILABLE;
            const isClickable = canSelect && seat.status === 'AVAILABLE';
            const seatLabel = `${seat.section} Row ${seat.rowLabel} Seat ${seat.seatNumber} — $${Number(seat.price).toFixed(2)} — ${isSelected ? 'Selected' : seat.status}`;

            return (
              <rect
                key={seat.id}
                x={seat.cx - SEAT_SIZE / 2}
                y={seat.cy - SEAT_SIZE / 2}
                width={SEAT_SIZE}
                height={SEAT_SIZE}
                rx={SEAT_RADIUS}
                ry={SEAT_RADIUS}
                fill={colors.fill}
                stroke={colors.stroke}
                strokeWidth={isSelected ? 2 : 1.5}
                style={{ cursor: isClickable ? 'pointer' : 'default', outline: 'none' }}
                role={isClickable ? 'button' : 'img'}
                aria-label={seatLabel}
                tabIndex={isClickable ? 0 : -1}
                onClick={(e) => handleSeatClick(e, seat)}
                onKeyDown={isClickable ? (e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleSeatClick(e, seat);
                  }
                } : undefined}
                onMouseEnter={(e) => handleSeatMouseEnter(e, seat)}
                onMouseLeave={handleSeatMouseLeave}
                onFocus={(e) => handleSeatMouseEnter(e, seat)}
                onBlur={handleSeatMouseLeave}
              />
            );
          })}
        </svg>

        {/* Tooltip */}
        {tooltip && (
          <div
            className="seat-map-tooltip"
            style={{ left: tooltip.x + 12, top: tooltip.y - 8 }}
          >
            <strong>{tooltip.seat.section}</strong> &mdash; Row {tooltip.seat.rowLabel}, Seat {tooltip.seat.seatNumber}
            <br />
            ${Number(tooltip.seat.price).toFixed(2)} &bull; {selectedSeatIds?.has(tooltip.seat.id) ? 'SELECTED' : tooltip.seat.status}
          </div>
        )}
      </div>

      {/* Legend */}
      <div className="seat-map-legend">
        {[
          { label: 'Available', key: 'AVAILABLE' },
          { label: 'Selected', key: 'SELECTED' },
          { label: 'Held', key: 'HELD' },
          { label: 'Sold / Booked', key: 'SOLD' },
        ].map(({ label, key }) => (
          <span key={key} className="seat-map-legend-item">
            <span
              className="seat-map-legend-swatch"
              style={{ background: STATUS_COLORS[key].fill, border: `2px solid ${STATUS_COLORS[key].stroke}` }}
            />
            {label}
          </span>
        ))}
      </div>
    </div>
  );
}

export default SeatMap;
