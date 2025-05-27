import { Motion } from 'capacitor-motion';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    Motion.echo({ value: inputValue })
}
