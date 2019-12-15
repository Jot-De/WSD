import React from "react";

import "./Tile.css";

class Tile extends React.Component {
  render() {
    const { type } = this.props;
    return <div className={`tile ${type}`}> </div>;
  }
}

export default Tile;
