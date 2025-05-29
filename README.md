# capacitor-motion

Capacitor Motion is a plugin that provides access to device motion and orientation data, including accelerometer, gyroscope, and magnetometer readings, for cross-platform mobile apps.

## Install

```bash
npm install native-capacitor-motion
npx cap sync
```

## API

<docgen-index>

* [`addListener('accel', ...)`](#addlisteneraccel-)
* [`addListener('orientation', ...)`](#addlistenerorientation-)
* [`addListener('heading', ...)`](#addlistenerheading-)
* [`removeAllListeners()`](#removealllisteners)
* [`startMotionUpdates()`](#startmotionupdates)
* [`stopMotionUpdates()`](#stopmotionupdates)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### addListener('accel', ...)

```typescript
addListener(eventName: 'accel', listenerFunc: AccelListener) => Promise<PluginListenerHandle>
```

Add a listener for accelerometer data

| Param              | Type                                                    |
| ------------------ | ------------------------------------------------------- |
| **`eventName`**    | <code>'accel'</code>                                    |
| **`listenerFunc`** | <code><a href="#accellistener">AccelListener</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.0.0

--------------------


### addListener('orientation', ...)

```typescript
addListener(eventName: 'orientation', listenerFunc: OrientationListener) => Promise<PluginListenerHandle>
```

Add a listener for device orientation change (compass heading, etc.)

| Param              | Type                                                                |
| ------------------ | ------------------------------------------------------------------- |
| **`eventName`**    | <code>'orientation'</code>                                          |
| **`listenerFunc`** | <code><a href="#orientationlistener">OrientationListener</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.0.0

--------------------


### addListener('heading', ...)

```typescript
addListener(eventName: 'heading', listenerFunc: HeadingListener) => Promise<PluginListenerHandle>
```

Add a listener for device heading change (compass heading, etc.)

| Param              | Type                                                        |
| ------------------ | ----------------------------------------------------------- |
| **`eventName`**    | <code>'heading'</code>                                      |
| **`listenerFunc`** | <code><a href="#headinglistener">HeadingListener</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.0.0

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Remove all the listeners that are attached to this plugin.

**Since:** 1.0.0

--------------------


### startMotionUpdates()

```typescript
startMotionUpdates() => Promise<void>
```

Start motion updates

**Since:** 1.0.0

--------------------


### stopMotionUpdates()

```typescript
stopMotionUpdates() => Promise<void>
```

Stop motion updates

**Since:** 1.0.0

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### AccelListenerEvent

| Prop                               | Type                                                  | Description                                                                                                                                                             | Since |
| ---------------------------------- | ----------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`acceleration`**                 | <code><a href="#acceleration">Acceleration</a></code> | An object giving the acceleration of the device on the three axis X, Y and Z. <a href="#acceleration">Acceleration</a> is expressed in m/s²                             | 1.0.0 |
| **`accelerationIncludingGravity`** | <code><a href="#acceleration">Acceleration</a></code> | An object giving the acceleration of the device on the three axis X, Y and Z with the effect of gravity. <a href="#acceleration">Acceleration</a> is expressed in m/s²  | 1.0.0 |
| **`rotationRate`**                 | <code><a href="#rotationrate">RotationRate</a></code> | An object giving the rate of change of the device's orientation on the three orientation axis alpha, beta and gamma. Rotation rate is expressed in degrees per seconds. | 1.0.0 |
| **`interval`**                     | <code>number</code>                                   | A number representing the interval of time, in milliseconds, at which data is obtained from the device.                                                                 | 1.0.0 |


#### Acceleration

| Prop    | Type                | Description                                  | Since |
| ------- | ------------------- | -------------------------------------------- | ----- |
| **`x`** | <code>number</code> | The amount of acceleration along the X axis. | 1.0.0 |
| **`y`** | <code>number</code> | The amount of acceleration along the Y axis. | 1.0.0 |
| **`z`** | <code>number</code> | The amount of acceleration along the Z axis. | 1.0.0 |


#### RotationRate

| Prop        | Type                | Description                                                      | Since |
| ----------- | ------------------- | ---------------------------------------------------------------- | ----- |
| **`alpha`** | <code>number</code> | The amount of rotation around the Z axis, in degrees per second. | 1.0.0 |
| **`beta`**  | <code>number</code> | The amount of rotation around the X axis, in degrees per second. | 1.0.0 |
| **`gamma`** | <code>number</code> | The amount of rotation around the Y axis, in degrees per second. | 1.0.0 |


#### Heading

| Prop          | Type                | Description                            | Since |
| ------------- | ------------------- | -------------------------------------- | ----- |
| **`heading`** | <code>number</code> | The heading of the device, in degrees. | 1.0.0 |


### Type Aliases


#### AccelListener

<code>(event: <a href="#accellistenerevent">AccelListenerEvent</a>): void</code>


#### OrientationListener

<code>(event: <a href="#rotationrate">RotationRate</a>): void</code>


#### OrientationListenerEvent

<code><a href="#rotationrate">RotationRate</a></code>


#### HeadingListener

<code>(event: <a href="#heading">Heading</a>): void</code>


#### HeadingListenerEvent

<code><a href="#heading">Heading</a></code>

</docgen-api>
