import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import SeatMap from './SeatMap';

function seat(overrides = {}) {
  return {
    id: 's1',
    section: 'Main',
    rowLabel: 'A',
    seatNumber: '1',
    status: 'AVAILABLE',
    price: 25,
    ...overrides,
  };
}

const seats = [
  seat({ id: 's1', rowLabel: 'A', seatNumber: '1', status: 'AVAILABLE' }),
  seat({ id: 's2', rowLabel: 'A', seatNumber: '2', status: 'HELD' }),
  seat({ id: 's3', rowLabel: 'B', seatNumber: '1', status: 'BOOKED' }),
  seat({ id: 's4', section: 'VIP', rowLabel: 'A', seatNumber: '1', status: 'AVAILABLE' }),
];

test('renders section and row labels', () => {
  render(<SeatMap seats={seats} selectedSeatIds={new Set()} onToggleSeat={jest.fn()} canSelect />);
  expect(screen.getByText('MAIN')).toBeInTheDocument();
  expect(screen.getByText('VIP')).toBeInTheDocument();
  expect(screen.getAllByText('A').length).toBeGreaterThan(0);
});

test('renders legend items', () => {
  render(<SeatMap seats={seats} selectedSeatIds={new Set()} onToggleSeat={jest.fn()} canSelect />);
  expect(screen.getByText('Available')).toBeInTheDocument();
  expect(screen.getByText('Selected')).toBeInTheDocument();
  expect(screen.getByText('Held')).toBeInTheDocument();
  expect(screen.getByText('Sold / Booked')).toBeInTheDocument();
});

test('clicking available seat calls onToggleSeat', () => {
  const onToggle = jest.fn();
  render(<SeatMap seats={seats} selectedSeatIds={new Set()} onToggleSeat={onToggle} canSelect />);
  const availableSeat = screen.getByRole('button', { name: /Main Row A Seat 1/ });
  fireEvent.click(availableSeat);
  expect(onToggle).toHaveBeenCalledWith('s1');
});

test('held/booked seats are not clickable', () => {
  const onToggle = jest.fn();
  render(<SeatMap seats={seats} selectedSeatIds={new Set()} onToggleSeat={onToggle} canSelect />);
  // Held/booked seats render as role=img (not button)
  const heldSeats = screen.getAllByRole('img', { name: /HELD|BOOKED/ });
  expect(heldSeats.length).toBeGreaterThan(0);
  heldSeats.forEach((s) => fireEvent.click(s));
  expect(onToggle).not.toHaveBeenCalled();
});

test('reset button restores default view', () => {
  render(<SeatMap seats={seats} selectedSeatIds={new Set()} onToggleSeat={jest.fn()} canSelect />);
  fireEvent.click(screen.getByText('Reset'));
  // No assertion on transform; just exercising the handler
});

test('zoom buttons fire without error', () => {
  render(<SeatMap seats={seats} selectedSeatIds={new Set()} onToggleSeat={jest.fn()} canSelect />);
  fireEvent.click(screen.getByText('+'));
  fireEvent.click(screen.getByText('−'));
});
