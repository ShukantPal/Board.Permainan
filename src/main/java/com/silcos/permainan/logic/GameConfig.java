package com.silcos.permainan.logic;

/**
 * {@link GameConfig} provides static utilities to manipulate
 * configuration settings encoded in an {@code int}.
 */
public final class GameConfig {

  /**
   * CAPTURE_OUTER_CIRCUIT_OPTIONAL flag allows the players to stop
   * their long move (along an outer circuit) midway, before actually
   * capturing the enemy piece.
   */
  public static int CAPTURE_OUTER_CIRCUIT_OPTIONAL = 0;
  
  /**
   *
   */
  public static int CAPTURE_INNER_CIRCUIT_OPTIONAL = 1;
  public static int DIAGONAL_STEPS_NOT_ALLOWED = 2;
  
  public static int newConfig() {
    return 0;
  }
  
  public static int newConfig(int ... flags) {
    int config = newConfig();
    
    for (int flag : flags) {
      config |= (1 << flag);
    }
    
    return config;
  }
  
  public static boolean getValue(int config, int flag) {
    return ((config >> flag) & 1) == 1;
  }
  
  public static int setValue(int config, int flag, boolean value) {
    if (value) {
      config |= (1 << flag);
    } else {
      config &= ~(1 << flag);
    }
  }
  
  private GameConfig() {
  }

}
