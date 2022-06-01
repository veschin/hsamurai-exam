prepare:
	yarn add shadow-cljs react react-dom create-react-class
	yarn install

prepare-npm:
	npm install shadow-cljs react react-dom create-react-class

release:
	rm -r -f .calva .cpcache .lsp classes build target/public resources/public/js
	mkdir classes
	clj -M:release
	clj -M:aot
	clj -M:uberjar
	rm -r -f classes

