include $(top_srcdir)/config/utility.am

cfinputsdir = $(SIPX_CFINPUTS)/plugin.d
dist_cfinputs_DATA = \
	hotdesking.cf


confdir = \
	$(SIPX_CONFDIR)/sipxhotdesking

dist_conf_DATA = \
	sipxhotdesking.xml \
	sipxhotdesking.properties

precommit : validate-xml

validate-xml : $(dist_conf_DATA)
	xmllint --noout $^

CLEANFILES = $(conf_DATA)

