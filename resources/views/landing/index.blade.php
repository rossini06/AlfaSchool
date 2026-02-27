@extends('layouts.landing')

@section('content')
    @include('landing.partials.hero')
    @include('landing.partials.problema')
    @include('landing.partials.solucao')
    @include('landing.partials.dashboard')
    @include('landing.partials.seguranca')
    @include('landing.partials.cta')
@endsection
