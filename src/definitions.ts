export interface MotionPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
