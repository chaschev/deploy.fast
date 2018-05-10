GC-free lightweight logger for Kotlin

## Motivation

Log4j is very good, but pain in the ass to configure

Focus of this project is on simplicity and speed

Takes 5 minutes to have multi-session routing logging to work (took me 3 days with Log4j)

Easy to extend and work with

Support modern language features:

 Generics, Closures, Multi-Platform (not yet!)
 
## Feature Set

SLF4J compliant with small speed deficiency, which will probably be fixed later and actually non-significant 

Classifiers VS Traditional markers for those who care. A good feature to have

With okLog you can actually log objects and use typed markers of your choice

We don't support some advanced features of Log4j like marker references out of the box. That being said you may 
implement them using your own Typed Classifiers. That is probably our TODO for a feature

## Performance 

The core is GC-free. Performance depends on features you include

Performance test shows 
 20m+ messages of writing into "nowhere" memory streams on my Macbook Pro '17
VS
 1m messages of writing into MemoryMapped files of Log4j2
 
So we don't bother about performance that much

