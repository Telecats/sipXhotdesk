include $(top_srcdir)/config/utility.am
include $(top_srcdir)/config/java.am

WSDL = $(abspath $(srcdir)/../../CallCenterService.wsdl)

noinst_DATA = javac-client

jardir = @SIPX_JAVADIR@/sipXrest/plugins
JAR_FILE = callcenter-interface.jar
jar_DATA = \
	$(JAR_FILE)

client_DEPS = \
	$(JAVAROOT) \
	$(call JavaDep,@SIPX_JAVADIR@/sipXcommons,axis)

# NOTE: This looks in builddir because src is generated
client_SRC = $(shell find nl -name '*.java')

javac-client : client-src

client-src : $(WSDL) Makefile
	$(call JavaCommand,client) org.apache.axis.wsdl.WSDL2Java \
          --NStoPkg http://www.telecats.nl/nl.telecats.sipxecs.callcenter.callCenterService=nl.telecats.sipxecs.callcenter.callCenterService \
	  $(WSDL)
	echo timestamp > $@

$(JAR_FILE) : javac-client Manifest.txt
	jar -cfm $@ Manifest.txt -C $(JAVAROOT) .

