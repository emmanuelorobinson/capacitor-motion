import { registerPlugin } from '@capacitor/core';

import type { MotionPlugin } from './definitions';

const Motion = registerPlugin<MotionPlugin>('Motion', {
  web: () => import('./web').then((m) => new m.MotionWeb()),
});

export * from './definitions';
export { Motion };
