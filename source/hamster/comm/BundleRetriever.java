/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm;

import java.util.ResourceBundle;

/**
 * Utility class with static methods allowing for the retrieving of ResourceBundles.
 * 
 * @author jdf19
 */
public class BundleRetriever
{
  /**
   * <p>Get the resource bundle name to be used in a call to {@link ResourceBundle#getBundle(String)}.
   * 
   * @param clas_s the class whose package contains the bundle.
   * @param bundleName the name of the bundle (without the .properties extension).
   * @return the string which can be used to call getBundle(...) with.
   */
  public static String retrieveBundleFromClassPackage(Class<?> clas_s, String bundleName)
  {
    String packageString = clas_s.getPackage().getName() + "." + bundleName;
    return packageString;
  }
}
