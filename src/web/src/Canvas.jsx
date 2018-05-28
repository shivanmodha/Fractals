import React, { Component } from "react";

class CanvasComponent extends Component {
    render() {
        return (
            <div className="Canvas-Wrapper" style={{ ...this.props.style }}>
                <canvas ref="canvas" width="100%" height="100%" style={{ border: "1px solid black" }} />
            </div>
        )
    }
    componentDidMount() {
        if (this.refs.canvas) {
            let canvasObj = new Canvas(this.refs.canvas);
            if (this.props.onLoad) {
                this.props.onLoad(canvasObj);
            }
        }
    }
}
class Color {
    constructor(r, g, b, a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = (a) ? (a) : (255);
    }
    render() {
        return "rgba(" + this.r + "," + this.g + "," + this.b + "," + this.a + ")";
    }
}
class HSLColor {
    constructor(h, s, l) {
        this.h = h;
        this.s = s;
        this.l = l;
    }
    render() {
        return "hsl(" + this.h + "," + this.s + "%," + this.l + "%)";
    }
}
class Canvas {
    constructor(reference) {
        this.context = reference.getContext("2d");
    }
    setColor(color) {
        this.context.fillStyle = color.render();
        this.context.strokeStyle = color.render();
    }
    FillRectangle(color, x, y, w, h) {
        this.setColor(color);
        this.context.rect(x, y, w, h);
        this.context.fill();
    }
    DrawRectangle(color, x, y, w, h) {
        this.setColor(color);
        this.context.rect(x, y, w, h);
        this.context.stroke();
    }
    SetPixel(color, x, y) {
        this.FillRectangle(color, x, y, 1, 1);
    }
}
export { Canvas, CanvasComponent, Color, HSLColor };