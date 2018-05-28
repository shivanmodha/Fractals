import React, { Component } from "react";
import { CanvasComponent, Canvas, Color, HSLColor } from "./Canvas.jsx";
class App extends Component {
    render() {
        return (
            <div className="App">
                <CanvasComponent style={{ width: "100%", height: "100%" }} onLoad={(canvas) => {
                    let resolutionWidth = 1000;
                    let resolutionHeight = 1000;
                    let iterations = 300;
                    let zoom = 13;
                    let xMul = 1;
                    let yMul = 1;
                    for (let i = 0; i < resolutionWidth; i++) {
                        for (let j = 0; j < resolutionHeight; j++) {
                            let x = (resolutionWidth / resolutionHeight) * (i - resolutionWidth / 2) / (0.5 * zoom * resolutionWidth);
                            let y = (j - resolutionHeight / 2) / (0.5 * zoom * resolutionHeight);
                            let counter = iterations;
                            while (x * x * xMul + y * y * yMul < 6 && counter > 0) {
                                let newX = x * x - y * y;
                                y = 2.0 * x * y;
                                x = newX;
                                counter--;
                            }
                            let c = new HSLColor(0, 100, 0);
                            if (counter > 0) {
                                c = new HSLColor((iterations / counter) % 1, 100, 100);
                            }
                            canvas.SetPixel(c, i, j);
                        }
                    }
                    this.setState({
                        Canvas: canvas
                    });
                }} />
            </div>
        );
    }
    componentDidMount() {
        
    }
}
export default App;
