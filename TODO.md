- 1.3.2

    - Inject parameter of type Reporter (move to net.jqwik.api)
        - Reporter.void publishReport(String key, Object objectToReport);
        - Document reporter in user guide

    - Allow specification of provider class in `@ForAll` and `@From`
      see https://github.com/jlink/jqwik/issues/91

    - Give all implementors of StreamableArbitrary their own arbitrary type
        - StreamArbitrary, IteratorArbitrary, ArrayArbitrary
        - Document Streamable arbitraries in user guide
        
- 1.3.x
        
    - `@Report(reportOnlyFailures = false)`

    - Guided Generation
      https://github.com/jlink/jqwik/issues/84
      - Maybe change AroundTryHook to allow replacement of `Random` source
      - Or: Introduce ProvideGenerationSourceHook
      
    - Re-implement shrinking so that it handles mutable objects correctly
    
    - Edge Cases
        - Arbitrary.withoutEdgeCases() 
            - should also work for individual generators
            - Maybe introduce ArbitraryDecorator or something like that
        
        - Arbitrary.addEdgeCase(value) 
            - Make shrinkable variants for
                - Numeric Arbitraries
                - CharacterArbitrary
                - Arbitrary.of() arbitraries
                - Collections
                - Combinators
            - Mixin edge cases in random order (https://github.com/jlink/jqwik/issues/101)

    - Change signature Arbitrary.exhaustive() -> ExhaustiveGenerator
    
    - Property runtime statistics (https://github.com/jlink/jqwik/issues/100)

    - Support more RandomDistribution modes, e.g. Log, PowerLaw
        https://en.wikipedia.org/wiki/Inverse_transform_sampling
        https://en.wikipedia.org/wiki/Ziggurat_algorithm
        https://github.com/jeffhain/jafaran/blob/master/src/main/java/net/jafaran/Ziggurat.java

    - @ResolveParameter method
        - Returns `Optional<MyType>` | `Optional<ParameterSupplier<MyType>>`
        - Optional Parameters: TypeUsage, LifecycleContext
        - static and non-static

    - PerProperty.Lifecycle
        - void beforeTry(TryLifecycleContext, parameters)
        - void afterTry(TryLifecycleContext, TryExecutionResult)
        - void onSatisfiedTry()
        - TryExecutionResult onFalsifiedTry(TryExecutionResult)

    - @StatisticsReportFormat
        - label=<statistics label> to specify for which statistics to use
        - Make it repeatable
