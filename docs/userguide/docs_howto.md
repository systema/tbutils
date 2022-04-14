# Documentation Build

tbutils documentation is build with [mkdocs](https://www.mkdocs.org/).

```bash
#pip install mkdocs
#pip install mkdocs-material
cd /d/projects/systema/iot/tb-utils/docs/userguide

#pip install markdown-include
#pip install pymdown-extensions # not needed  

# workaround for  https://github.com/mkdocs/mkdocs/issues/2469
#pip install -Iv importlib_metadata==4.5.0

#mkdocs new .

mkdocs serve

mkdocs build
```

For more details see <https://squidfunk.github.io/mkdocs-material/creating-your-site/>


## Tech Pointers

For publishing options see <https://squidfunk.github.io/mkdocs-material/publishing-your-site/>

Nice options overview <https://github.com/squidfunk/mkdocs-material/blob/master/mkdocs.yml>

include code into mkdocs  <https://github.com/mkdocs/mkdocs/issues/777> <https://github.com/cmacmackin/markdown-include>

header stripping ? Not yet, see <https://github.com/cmacmackin/markdown-include/issues/9>

**{todo}** consider using snippets <https://squidfunk.github.io/mkdocs-material/reference/code-blocks/#snippets>

