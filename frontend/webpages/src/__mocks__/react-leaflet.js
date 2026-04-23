const React = require('react');
const MapContainer = ({ children }) => React.createElement(React.Fragment, null, children);
const TileLayer = () => null;
const Marker = ({ children }) => React.createElement(React.Fragment, null, children);
const Popup = ({ children }) => React.createElement(React.Fragment, null, children);
module.exports = { MapContainer, TileLayer, Marker, Popup };
