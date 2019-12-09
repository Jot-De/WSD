import React from "react";

import "./Tile.css";

class Tile extends React.Component {
  render() {
    const { name, type, location } = this.props;
    console.log(type);
    return <div className={`tile ${type}`}> </div>;
  }
}

export default Tile;
