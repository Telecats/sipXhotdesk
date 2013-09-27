include $(top_srcdir)/config/utility.am

## Libtool version info - update only immediately before a release
# Current - increment if any interfaces are added, removed, or changed
version_Current=1
# Revision - set to 0 if Current was incremented
#            increment if -lmongoclient \
    -lboost_system-mtany implementation has changed
version_Revision=0
# Age - set to 0 of any interfaces have been removed
#       increment if any interfaces have been added
version_Age=0

authpluginsdir = $(SIPX_LIBDIR)/authplugins
authplugins_LTLIBRARIES = \
	libHotdesking.la

libHotdesking_la_SOURCES = \
	Hotdesking.h \
	Hotdesking.cpp

libHotdesking_la_CXXFLAGS = \
	-I$(top_builddir)/config \
	-I$(top_srcdir)/include \
	-DSIPX_CONFDIR=\"@SIPX_CONFDIR@\"

libHotdesking_la_LDFLAGS = \
	-module \
	-version-info ${version_Current}:${version_Revision}:${version_Age}

libHotdesking_la_LIBADD = \
	@SIPXPORT_LIBS@ \
	@SIPXCOMMSERVER_LIBS@
