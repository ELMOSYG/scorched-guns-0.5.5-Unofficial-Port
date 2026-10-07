package top.ribs.scguns.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import top.ribs.scguns.ScorchedGuns;

/**
 * Shoulder Surfing Reloaded compatibility.
 *
 * <p>The original only ever touched that mod through reflection - except for the
 * {@code Perspective} enum, which it imported to build the value passed to the
 * reflective call. Since no 1.21.1 build of Shoulder Surfing is in this
 * project's dependency set, the enum is resolved reflectively too, which removes
 * the compile time dependency while keeping the feature working when the mod is
 * present.</p>
 */
public class ShoulderSurfingHelper {
    private static final String PERSPECTIVE_CLASS =
            "com.github.exopandora.shouldersurfing.api.model.Perspective";

    private static boolean disable1 = false;
    private static boolean disable2 = false;
    private static Method getShoulderInstance;
    private static Method isShoulderSurfing;
    private static Method changePerspective;
    private static Class<?> perspectiveClass;

    public ShoulderSurfingHelper() {
        super();
    }

    public static boolean isShoulderSurfing() {
        if (!ScorchedGuns.shoulderSurfingLoaded) {
            return false;
        } else {
            if (!disable1) {
                try {
                    init();
                    Object object = getShoulderInstance.invoke(null);
                    return (Boolean) isShoulderSurfing.invoke(object);
                } catch (IllegalAccessException | IllegalArgumentException | NullPointerException | InvocationTargetException var2) {
                    ScorchedGuns.LOGGER.error("Shoulder Surfing helper error with method isShoulderSurfing!");
                    disable1 = true;
                }
            } else if (!disable2) {
                try {
                    init();
                    Object object = getShoulderInstance.invoke(null);
                    return (Boolean) isShoulderSurfing.invoke(object);
                } catch (IllegalAccessException | IllegalArgumentException | NullPointerException | InvocationTargetException var1) {
                    ScorchedGuns.LOGGER.error("Shoulder Surfing helper error with method isShoulderSurfing!");
                    disable2 = true;
                }
            }

            return false;
        }
    }

    public static void changePerspective(String perspective) {
        if (ScorchedGuns.shoulderSurfingLoaded) {
            if (!disable1 || !disable2) {
                try {
                    init();
                    Object pov = perspectiveConstant(perspective);
                    Object object = getShoulderInstance.invoke(null);
                    changePerspective.invoke(object, pov);
                } catch (IllegalAccessException | NullPointerException | InvocationTargetException var4) {
                    ScorchedGuns.LOGGER.error("Shoulder Surfing helper error with method changePerspective!");
                    if (!disable1) {
                        disable1 = true;
                    } else {
                        disable2 = true;
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object perspectiveConstant(String perspective) throws NullPointerException {
        String name = "SHOULDER_SURFING";
        String upper = perspective.toUpperCase();
        if (upper.equals("FIRST_PERSON")) {
            name = "FIRST_PERSON";
        } else if (upper.equals("THIRD_PERSON_BACK")) {
            name = "THIRD_PERSON_BACK";
        } else if (upper.equals("THIRD_PERSON_FRONT")) {
            name = "THIRD_PERSON_FRONT";
        }
        if (perspectiveClass == null) {
            throw new NullPointerException("perspective class not loaded");
        }
        return Enum.valueOf((Class<? extends Enum>) perspectiveClass.asSubclass(Enum.class), name);
    }

    private static void init() {
        if (getShoulderInstance == null) {
            perspectiveClass = loadClass(PERSPECTIVE_CLASS);
            if (perspectiveClass == null) {
                disable1 = true;
                disable2 = true;
                ScorchedGuns.LOGGER.info("Shoulder Surfing Reloaded is not installed, proceeding without helper.");
                return;
            }
            Class<?>[] pArg = new Class[]{perspectiveClass};
            try {
                Class<?> shoulderSurfingImpl = Class.forName("com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl");
                getShoulderInstance = shoulderSurfingImpl.getDeclaredMethod("getInstance");
                isShoulderSurfing = shoulderSurfingImpl.getDeclaredMethod("isShoulderSurfing");
                changePerspective = shoulderSurfingImpl.getDeclaredMethod("changePerspective", pArg);
            } catch (NoSuchMethodException | NullPointerException | ClassNotFoundException var3) {
                disable1 = true;
            }

            if (disable1) {
                try {
                    Class<?> shoulderInstance = Class.forName("com.github.exopandora.shouldersurfing.client.ShoulderInstance");
                    getShoulderInstance = shoulderInstance.getDeclaredMethod("getInstance");
                    isShoulderSurfing = shoulderInstance.getDeclaredMethod("doShoulderSurfing");
                    changePerspective = shoulderInstance.getDeclaredMethod("changePerspective", pArg);
                } catch (NoSuchMethodException | NullPointerException | ClassNotFoundException var2) {
                    disable2 = true;
                }
            }

            if (disable1 && disable2) {
                ScorchedGuns.LOGGER.info("Shoulder Surfing Reloaded is not installed, proceeding without helper.");
            }
        }
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
