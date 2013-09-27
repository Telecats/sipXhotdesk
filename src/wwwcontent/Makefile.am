
hotdeskingprompts_endir = @wwwdir@/doc/stdprompts_en/hotdesking
dist_hotdeskingprompts_en_DATA = \
	$(shell cd $(srcdir); find stdprompts_en/hotdesking -maxdepth 1 -name '*.wav')

install-data-hook:
	cd $(DESTDIR)@wwwdir@/doc; 
	[ -L stdprompts ] || ln -s stdprompts_en stdprompts
