import type { PluginListenerHandle } from '@capacitor/core';

export interface MotionPlugin {
  /**
   * Add a listener for accelerometer data
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'accel',
    listenerFunc: AccelListener,
  ): Promise<PluginListenerHandle>;

  /**
   * Add a listener for device orientation change (compass heading, etc.)
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'orientation',
    listenerFunc: OrientationListener,
  ): Promise<PluginListenerHandle>;

  /**
   * Add a listener for device heading change (compass heading, etc.)
   *
   * @since 1.0.0
   */
  addListener(
    eventName: 'heading',
    listenerFunc: HeadingListener,
  ): Promise<PluginListenerHandle>;

  /**
   * Remove all the listeners that are attached to this plugin.
   *
   * @since 1.0.0
   */
  removeAllListeners(): Promise<void>;

  /**
   * Start motion updates
   *
   * @since 1.0.0
   */
  startMotionUpdates(): Promise<void>;

  /**
   * Stop motion updates
   *
   * @since 1.0.0
   */
  stopMotionUpdates(): Promise<void>;
}

export type AccelListener = (event: AccelListenerEvent) => void;
export type OrientationListener = (event: OrientationListenerEvent) => void;
export type OrientationListenerEvent = RotationRate;
export type HeadingListener = (event: HeadingListenerEvent) => void;
export type HeadingListenerEvent = Heading;

export interface RotationRate {
  /**
   * The amount of rotation around the Z axis, in degrees per second.
   *
   * @since 1.0.0
   */
  alpha: number;

  /**
   * The amount of rotation around the X axis, in degrees per second.
   *
   * @since 1.0.0
   */
  beta: number;

  /**
   * The amount of rotation around the Y axis, in degrees per second.
   *
   * @since 1.0.0
   */
  gamma: number;
}

export interface Heading {
  /**
   * The heading of the device, in degrees.
   *
   * @since 1.0.0
   */
  heading: number;
}

export interface Acceleration {
  /**
   * The amount of acceleration along the X axis.
   *
   * @since 1.0.0
   */
  x: number;

  /**
   * The amount of acceleration along the Y axis.
   *
   * @since 1.0.0
   */
  y: number;

  /**
   * The amount of acceleration along the Z axis.
   *
   * @since 1.0.0
   */
  z: number;
}

export interface AccelListenerEvent {
  /**
   * An object giving the acceleration of the device on the three axis X, Y and Z. Acceleration is expressed in m/s²
   *
   * @since 1.0.0
   */
  acceleration: Acceleration;

  /**
   * An object giving the acceleration of the device on the three axis X, Y and Z with the effect of gravity. Acceleration is expressed in m/s²
   *
   * @since 1.0.0
   */
  accelerationIncludingGravity: Acceleration;

  /**
   * An object giving the rate of change of the device's orientation on the three orientation axis alpha, beta and gamma. Rotation rate is expressed in degrees per seconds.
   *
   * @since 1.0.0
   */
  rotationRate: RotationRate;

  /**
   * A number representing the interval of time, in milliseconds, at which data is obtained from the device.
   *
   * @since 1.0.0
   */
  interval: number;
}
