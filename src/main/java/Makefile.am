include $(top_srcdir)/config/utility.am
include $(top_srcdir)/config/java.am
include $(top_srcdir)/common.am

EXTRA_DIST = \
        $(sipxhotdesking_SRC) \
		$(sipxhotdesking_RESOURCES)

noinst_DATA = javac-sipxhotdesking

jardir = @SIPX_JAVADIR@/hotdesking
JAR_FILE = hotdeskingTc.jar
jar_DATA = \
	$(JAR_FILE)

$(JAR_FILE) : javac-sipxhotdesking
	jar -cf $@ \
	$(call JarInclude,$(JAVAROOT),.) \
	$(call JarInclude,$(srcdir),$(sipxhotdesking_RESOURCES))


sipxhotdesking_SRC = \
	$(shell cd $(srcdir); find org -type f -name '*.java')

sipxhotdesking_RESOURCES = \
        org/sipfoundry/hotdesking/HotdeskingCode.properties \
        org/sipfoundry/hotdesking/mongo/mongo.beans.xml \
        org/sipfoundry/hotdesking/hotdesking.beans.xml

sipxhotdesking_DEPS = \
	$(JAVAROOT) \
	$(call JavaDep, @SIPX_JAVADIR@/sipXcommons, $(sipxhotdesking_PKGS)) \
	@SIPX_JAVADIR@/sipXivr/sipXivr.jar

