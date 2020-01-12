import React from "react";

import "./Tile.css";

class Tile extends React.Component {
  render() {
    const { type, isParking, isCar, freeSlots } = this.props;

    return (
      <div
        className={`tile ${type} ${isParking ? "parking" : ""} ${
          isCar ? "car" : ""
        }`}
      >
        {freeSlots ? freeSlots : ""}
      </div>
    );
  }
}

export default Tile;
