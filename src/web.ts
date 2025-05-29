import { WebPlugin } from '@capacitor/core';
import type { MotionPlugin, AccelListener, OrientationListener, HeadingListener } from './definitions';

export class MotionWeb extends WebPlugin implements MotionPlugin {
  private accelListeners: AccelListener[] = [];
  private orientationListeners: OrientationListener[] = [];
  private headingListeners: HeadingListener[] = [];
  private isMotionActive = false;
  private motionHandler: ((event: DeviceMotionEvent) => void) | null = null;
  private orientationHandler: ((event: DeviceOrientationEvent) => void) | null = null;
  private headingHandler: ((event: GeolocationPosition) => void) | null = null;

  async addListener(eventName: 'accel' | 'orientation' | 'heading', listenerFunc: any) {
    if (eventName === 'accel') {
      this.accelListeners.push(listenerFunc);
      await this.startAccelerometer();
    } else if (eventName === 'heading') {
      this.headingListeners.push(listenerFunc);
      await this.startHeading();
    } else if (eventName === 'orientation') {
      this.orientationListeners.push(listenerFunc);
      await this.startOrientation();
    }

    return {
      remove: async () => {
        if (eventName === 'accel') {
          const index = this.accelListeners.indexOf(listenerFunc);
          if (index > -1) {
            this.accelListeners.splice(index, 1);
          }
          if (this.accelListeners.length === 0) {
            this.stopAccelerometer();
          }
        } else if (eventName === 'orientation') {
          const index = this.orientationListeners.indexOf(listenerFunc);
          if (index > -1) {
            this.orientationListeners.splice(index, 1);
          }
          if (this.orientationListeners.length === 0) {
            this.stopOrientation();
          }
        }
      }
    };
  }

  async removeAllListeners() {
    this.accelListeners = [];
    this.orientationListeners = [];
    this.stopAccelerometer();
    this.stopOrientation();
  }

  async startMotionUpdates() {
    // Web implementation doesn't need explicit start
  }

  async stopMotionUpdates() {
    this.stopAccelerometer();
    this.stopOrientation();
  }

  private async startAccelerometer() {
    if (this.isMotionActive) return;

    // Request permission for iOS Safari
    if (typeof (DeviceMotionEvent as any).requestPermission === 'function') {
      const permission = await (DeviceMotionEvent as any).requestPermission();
      if (permission !== 'granted') {
        throw new Error('Motion permission denied');
      }
    }

    this.motionHandler = (event: DeviceMotionEvent) => {
      const accelEvent = {
        acceleration: {
          x: event.acceleration?.x || 0,
          y: event.acceleration?.y || 0,
          z: event.acceleration?.z || 0,
        },
        accelerationIncludingGravity: {
          x: event.accelerationIncludingGravity?.x || 0,
          y: event.accelerationIncludingGravity?.y || 0,
          z: event.accelerationIncludingGravity?.z || 0,
        },
        rotationRate: {
          alpha: event.rotationRate?.alpha || 0,
          beta: event.rotationRate?.beta || 0,
          gamma: event.rotationRate?.gamma || 0,
        },
        interval: event.interval || 16.67, // ~60fps default
      };

      this.accelListeners.forEach(listener => listener(accelEvent));
    };

    window.addEventListener('devicemotion', this.motionHandler);
    this.isMotionActive = true;
  }

  private async startOrientation() {
    // Request permission for iOS Safari
    if (typeof (DeviceOrientationEvent as any).requestPermission === 'function') {
      const permission = await (DeviceOrientationEvent as any).requestPermission();
      if (permission !== 'granted') {
        throw new Error('Orientation permission denied');
      }
    }

    this.orientationHandler = (event: DeviceOrientationEvent) => {
      const orientationEvent = {
        alpha: event.alpha || 0,
        beta: event.beta || 0,
        gamma: event.gamma || 0,
      };

      this.orientationListeners.forEach(listener => listener(orientationEvent));
    };

    window.addEventListener('deviceorientation', this.orientationHandler);
  }

  private async startHeading() {
    this.headingHandler = (event: GeolocationPosition) => {
      const headingEvent = {
        heading: event.coords.heading || 0,
      };

      this.headingListeners.forEach(listener => listener(headingEvent));
    };

    navigator.geolocation.watchPosition(this.headingHandler);
  }

  private stopAccelerometer() {
    if (this.motionHandler) {
      window.removeEventListener('devicemotion', this.motionHandler);
      this.motionHandler = null;
      this.isMotionActive = false;
    }
  }

  private stopOrientation() {
    if (this.orientationHandler) {
      window.removeEventListener('deviceorientation', this.orientationHandler);
      this.orientationHandler = null;
    }
  }
}
