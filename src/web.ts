import { WebPlugin } from '@capacitor/core';

import type { MotionPlugin } from './definitions';

export class MotionWeb extends WebPlugin implements MotionPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
