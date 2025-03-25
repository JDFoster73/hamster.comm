package hamster.comm.server;

import java.util.ResourceBundle;

/**
 * <p>Simple utility class to help retrieve resource bundles from the internal codebase.
 * 
 * @author jdf19
 *
 */
public class BundleHelper
{
  /**
   * <p>Get the given bundle by name from the package which contains the given class.
   * 
   * @param clas_s the class from whose package the bundle will be retrieved from
   * @param bundleName the name of the bundle (<b>without</b> the .properties extension!)
   * @return the resource bundle retrieved as a result.
   */
  public static ResourceBundle retrieveBundleFromClassPackage(Class<?> clas_s, String bundleName)
  {
    //Get the package from the class.
    //Add the bundle name to it.
    String packageString = clas_s.getPackage().getName() + "." + bundleName;
    
    //Return the bundle name.
    return ResourceBundle.getBundle(packageString);
  }

}
