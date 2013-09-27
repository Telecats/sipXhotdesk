include $(top_srcdir)/config/utility.am
include $(top_srcdir)/config/java.am
include $(top_srcdir)/common.am

EXTRA_DIST = \
        $(hotdeskingtrigger_SRC) \
		$(hotdeskingtrigger_RESOURCES)

noinst_DATA = javac-hotdeskingtrigger

jardir = @SIPX_JAVADIR@/sipXrest/plugins
JAR_FILE = hotdeskingtrigger.jar
jar_DATA = \
	$(JAR_FILE)

$(JAR_FILE) : javac-hotdeskingtrigger Manifest.txt $(hotdeskingtrigger_RESOURCES)
	jar -cfm $@ Manifest.txt \
	$(call JarInclude,$(JAVAROOT),.) \
	$(call JarInclude,$(srcdir),$(hotdeskingtrigger_RESOURCES))


hotdeskingtrigger_SRC = \
	$(shell cd $(srcdir); find org -type f -name '*.java')

hotdeskingtrigger_RESOURCES = \
	$(shell cd $(srcdir); find . -type f -name '*.xml')
#        org/sipfoundry/hotdesking/HotdeskingCode.properties \
#        org/sipfoundry/hotdesking/mongo/mongo.beans.xml \
#        org/sipfoundry/hotdesking/hotdesking.beans.xml

hotdeskingtrigger_DEPS = \
	$(JAVAROOT) \
	@SIPX_JAVADIR@/sipXrest/sipxrest.jar \
	$(call JavaDep, @SIPX_JAVADIR@/sipXcommons, $(hotdeskingtrigger_PKGS)) \
	../lib
##	@SIPX_JAVADIR@/sipXrest/plugins/callcenter-interface.jar

