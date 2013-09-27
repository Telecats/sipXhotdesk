AC_PREREQ(2.57)
AC_INIT(sipXhotdesking, 4.6.0, sipx-dev@list.sipfoundry.org)
AC_CONFIG_AUX_DIR(config)
m4_include([config/general.m4])
m4_include([config/sipXlib.m4])

#common macros
SFAC_INIT_FLAGS

# cpp macros
AC_CONFIG_SRCDIR([proxy-authplugin/Hotdesking.cpp])
AM_CONFIG_HEADER([proxy-authplugin/config.h])
AM_DISABLE_STATIC
AC_PROG_LIBTOOL
AC_PROG_CXX
AC_PROG_CC
AX_COMPILER_VENDOR
SFAC_INIT_FLAGS
SFAC_LIB_PORT
SFAC_LIB_COMMSERVER
#additional to get the proxy project imported
#CHECK_XARGS_REPLACE
#AM_CONDITIONAL(IS_RPMBUILD, test x$enable_rpmbuild = xyes)


# java macros
m4_include([config/java.m4])
m4_include([config/java2.m4])
AM_INIT_AUTOMAKE(foreign tar-ustar)
SFAC_AUTOMAKE_VERSION([1.6])
#AC_CONFIG_SRCDIR([src/main/java/org/sipfoundry/hotdesking/HotdeskingService.java])
AC_PATH_PROG([BASH],[bash])
AC_PROG_JAVA_CC([javac])
AC_PROG_JAVA([java])
PROG_JAVA_DEP
AC_SUBST(SIPX_IS_SRC, [true])
if test "${enable_rpmbuild}" == "yes" ; then
  SIPX_IS_SRC=false
fi

AC_CONFIG_FILES([
    Makefile
    etc/Makefile
    bin/Makefile
    src/Makefile
    src/main/Makefile
    src/main/java/Makefile
    src/wwwcontent/Makefile
    config-plugin/Makefile
    proxy-authplugin/Makefile
    rest-plugin/Makefile
    rest-plugin/lib/Makefile
    rest-plugin/src/Makefile
])

AC_OUTPUT
